package com.auction.auctionorchestration;

import com.auction.auctionorchestration.dto.AutoBidRequest;
import com.auction.auctionorchestration.dto.BidPostRequest;
import com.auction.bids.AutoBid;
import com.auction.bids.Bid;
import com.auction.bids.BidService;
import com.auction.common.BaseException;
import com.auction.common.BaseObjectResponse;
import com.auction.common.BaseResponse;
import com.auction.common.ItemPricesSink;
import com.auction.common.jointdata.BidAndItem;
import com.auction.items.Item;
import com.auction.items.ItemService;
import com.auction.itemstatus.ItemStatus;
import com.auction.itemstatus.ItemStatusService;
import com.auction.users.User;
import com.auction.users.UserService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service cốt lõi quản lý toàn bộ quy trình nghiệp vụ của phiên đấu giá (Auction Orchestration).
 * Điều phối đặt cược thủ công, tự động phản hồi đấu giá (Auto-Bid), hoàn trả tiền cược cũ,
 * kết thúc phiên đấu giá cược sớm (Buy It Now), gia hạn bù giờ (Anti-Sniper) và kiểm tra ví tài khoản.
 */
@Service
public class AuctionService {

    private final ItemService itemService;
    private final UserService userService;
    private final ItemStatusService itemStatusService;
    private final BidService bidService;
    private final ItemPricesSink itemPricesSink;

    // Cấu hình thời gian bù giờ cho cơ chế chống bắn tỉa (Anti-Sniper) từ application.properties (tính bằng mili-giây)
    @Value("${extra_time}")
    private Long extraTime;

    public AuctionService(
        ItemService itemService,
        UserService userService,
        ItemStatusService itemStatusService,
        BidService bidService,
        ItemPricesSink itemPricesSink
    ) {
        this.itemService = itemService;
        this.userService = userService;
        this.itemStatusService = itemStatusService;
        this.bidService = bidService;
        this.itemPricesSink = itemPricesSink;
    }

    /**
     * Xử lý luồng đặt cược (bid) thủ công từ phía người dùng.
     * Tích hợp cơ chế tự động trả giá (Auto-Bid) để giải quyết xung đột khi có người dùng khác đang đặt Auto-Bid cho sản phẩm.
     *
     * @param request  Thông tin yêu cầu đặt cược bao gồm itemId và số tiền đặt cược (bidAmount)
     * @param username Tên đăng nhập của người thực hiện đặt cược
     * @return Phản hồi BaseObjectResponse chứa thông tin chi tiết về lượt cược (Bid) được tạo
     */
    @Transactional
    public BaseObjectResponse<Bid> createBid(
        BidPostRequest request,
        String username
    ) {
        Bid bid;
        Item item = itemService.getItemRef(request.itemId());
        User user = userService.getUserRef(username);

        // Lấy trạng thái hiện tại của phiên đấu giá cho sản phẩm này
        ItemStatus itemStatus = itemStatusService.getItemStatus(
            request.itemId()
        );

        // Kiểm tra các điều kiện nghiệp vụ cơ bản trước khi đặt cược
        validateBasicBidRequirement(
            item,
            user,
            itemStatus,
            request.bidAmount()
        );

        // Kiểm tra xem người dùng hiện tại đã từng đặt cược cho sản phẩm này chưa.
        // Nếu đã từng cược, cập nhật số tiền cược mới. Nếu chưa, tạo một lượt cược mới.
        if (bidService.existUserAndItem(user, item)) {
            bid = bidService.getBidByUserAndItem(user, item);
            bid.setBidAmount(request.bidAmount());
            bidService.saveBid(bid);
        } else {
            bid = new Bid(item, user, request.bidAmount());
            bidService.saveBid(bid);
        }

        // Tìm cấu hình tự động đấu giá (Auto-Bid) đang chạy trên sản phẩm này
        Optional<AutoBid> autoBidOP = bidService.getAutoBidByItemId(
            request.itemId()
        );

        // Trường hợp 1: Đã tồn tại một cấu hình Auto-Bid của người khác hoạt động trên sản phẩm này
        if (
            autoBidOP.isPresent() &&
            !autoBidOP.get().getBidder().getUsername().equals(username)
        ) {
            AutoBid autoBid = autoBidOP.get();

            // Nếu số tiền cược thủ công mới cộng với bước giá (bidIncrement) vượt quá giới hạn tối đa của Auto-Bid
            // Người đặt cược thủ công thắng Auto-Bid hiện tại.
            if (request.bidAmount() + itemStatus.getBidIncrement() > autoBid.getMaxBidLimit()) {
                User autoUser = autoBid.getBidder();
                // Hoàn lại toàn bộ giới hạn tối đa của Auto-Bid cho người dùng bị đánh bại
                userService.addBalance(autoUser.getUsername(), autoBid.getMaxBidLimit());
                // Khấu trừ số tiền đặt cược mới của người cược thủ công
                userService.deductBalance(username, request.bidAmount());

                // Cập nhật người cược thủ công thành người dẫn đầu phiên đấu giá hiện tại
                updateItemStatusHighestBidder(
                    itemStatus,
                    username,
                    request.bidAmount()
                );
            } else {
                // Ngược lại, Auto-Bid tự động trả giá cao hơn để giữ vị trí dẫn đầu.
                // Giá cược tự động mới bằng giá trị nhỏ hơn giữa: (giá cược thủ công + bước giá) hoặc (giới hạn tối đa của Auto-Bid).
                double autoCounter = Math.min(
                    request.bidAmount() + itemStatus.getBidIncrement(),
                    autoBid.getMaxBidLimit()
                );

                // Cập nhật mức cược hiện tại của Auto-Bid và lưu lại vào DB
                autoBid.setCurrentBidValue(autoCounter);
                bidService.saveAutoBid(autoBid);

                // Giữ nguyên người cài đặt Auto-Bid làm người dẫn đầu phiên đấu giá với giá cược tự động mới
                updateItemStatusHighestBidder(
                    itemStatus,
                    autoBid.getBidder().getUsername(),
                    autoCounter
                );
            }
        } else {
            // Trường hợp 2: Không có Auto-Bid của người khác hoạt động trên sản phẩm này
            // Khấu trừ tiền đặt cược của người cược thủ công mới
            userService.deductBalance(username, request.bidAmount());

            // Hoàn trả lại số tiền cược trước đó cho người dẫn đầu cũ (nếu có và không phải là người tạo ra sản phẩm/chủ sở hữu)
            if (
                !itemStatus
                    .getHighestBidUser()
                    .equals(item.getUser().getUsername())
            ) {
                userService.addBalance(itemStatus.getHighestBidUser(), itemStatus.getCurrentPrice());
            }

            // Cập nhật người cược mới làm người dẫn đầu phiên đấu giá
            updateItemStatusHighestBidder(
                itemStatus,
                username,
                request.bidAmount()
            );
        }

        // Áp dụng cơ chế Anti-Sniper gia hạn thêm thời gian nếu đặt cược sát giờ kết thúc
        applyAntiBidExtension(itemStatus);

        // Lưu trạng thái đấu giá mới và phát sóng (publish) giá mới qua Server-Sent Events (SSE)
        itemStatusService.saveStatus(itemStatus);
        itemPricesSink.publishPrice(
            request.itemId(),
            itemStatus.getCurrentPrice()
        );
        return new BaseObjectResponse<>(
            true,
            "Successfully created bid for an item",
            bid
        );
    }

    /**
     * Lấy danh sách phân trang các lượt đặt cược hiện tại của người dùng.
     *
     * @param username Tên đăng nhập của người dùng cần lấy lịch sử
     * @param page     Số thứ tự trang cần lấy (0-indexed)
     * @param size     Kích thước bản ghi trên mỗi trang
     * @return Phản hồi chứa trang kết quả các lượt cược
     */
    @Transactional(readOnly = true)
    public BaseObjectResponse<Page<Bid>> getMyCurrentBids(
        String username,
        int page,
        int size
    ) {
        PageRequest pageable = PageRequest.of(page, size);
        User userRef = userService.getUserRef(username);

        Page<Bid> bids = bidService.getAllUserBid(userRef, pageable);

        return new BaseObjectResponse<Page<Bid>>(
            true,
            "succesfully got my bids",
            bids
        );
    }

    /**
     * Lấy danh sách toàn bộ các sản phẩm đấu giá mà người dùng hiện tại đã thắng cuộc.
     *
     * @param username Tên đăng nhập của người dùng
     * @return Phản hồi chứa danh sách cặp đối tượng cược thắng và sản phẩm
     */
    @Transactional(readOnly = true)
    public BaseObjectResponse<List<BidAndItem>> getMyWinnings(String username) {
        List<Bid> bids = bidService.getUserWins(username);
        ArrayList<BidAndItem> items = new ArrayList<BidAndItem>();
        for (Bid bid : bids) {
            items.add(new BidAndItem(bid, bid.getItem()));
        }
        return new BaseObjectResponse<List<BidAndItem>>(
            true,
            "successfully returned winnings",
            items
        );
    }

    /**
     * Thực hiện tính năng "Mua ngay" (Buy It Now) của sản phẩm.
     * Trả thẳng giá mua đứt và kết thúc phiên đấu giá sản phẩm này ngay lập tức.
     *
     * @param itemId   Mã ID sản phẩm cần mua đứt
     * @param username Tên đăng nhập của người mua đứt
     * @return Phản hồi thông báo mua thành công
     */
    @Transactional
    public BaseResponse buyItemNow(Long itemId, String username) {
        ItemStatus itemStatus = itemStatusService.getItemStatus(itemId);
        User user = userService.getUserByUsername(username);
        Item item = itemService.getItem(itemId);
        
        // Kiểm tra các ràng buộc cược cơ bản bằng mức giá mua đứt
        validateBasicBidRequirement(
            item,
            user,
            itemStatus,
            itemStatus.getBuyItNowPrice()
        );

        // Lưu thông tin lượt cược mua đứt vào DB và trừ tiền người mua
        Bid bid = new Bid(item, user, itemStatus.getBuyItNowPrice());
        bidService.saveBid(bid);
        userService.deductBalance(username, itemStatus.getBuyItNowPrice());

        // Hoàn trả lại số tiền cho người giữ vị trí cao nhất trước đó (nếu có)
        if (
            !itemStatus.getHighestBidUser().equals(item.getUser().getUsername())
        ) {
            userService.addBalance(itemStatus.getHighestBidUser(), itemStatus.getCurrentPrice());
        }

        // Cập nhật người dẫn đầu cao nhất mới và đặt thời gian kết thúc đấu giá về thời điểm hiện tại (đóng phiên ngay lập tức)
        updateItemStatusHighestBidder(
            itemStatus,
            username,
            itemStatus.getBuyItNowPrice()
        );
        itemStatus.setEndTime(Instant.now().toEpochMilli());
        itemStatusService.saveStatus(itemStatus);

        // Phát sóng cập nhật giá mua đứt
        itemPricesSink.publishPrice(itemId, itemStatus.getBuyItNowPrice());

        return new BaseResponse(true, "Successfully bought item");
    }

    /**
     * Thiết lập cấu hình tự động đặt giá (Auto-Bid) cho một sản phẩm đấu giá.
     * Tự động hoàn trả hoặc trừ thêm tiền khi người dùng cập nhật hạn mức tối đa của mình.
     * Đồng thời giải quyết tranh chấp trực tiếp nếu đã có một Auto-Bid khác của đối thủ trên sản phẩm.
     *
     * @param request    Thông tin cấu hình Auto-Bid gồm itemId và hạn mức tối đa maxBidLimit
     * @param bidderName Tên đăng nhập của người dùng thiết lập Auto-Bid
     * @return Phản hồi trạng thái thiết lập thành công
     */
    @Transactional
    public BaseResponse createAutoBid(
        AutoBidRequest request,
        String bidderName
    ) {
        User bidder = userService.getUserByUsername(bidderName);
        Optional<AutoBid> autoBidOP = bidService.getAutoBidByItemId(
            request.itemId()
        );
        ItemStatus itemStatus = itemStatusService.getItemStatus(
            request.itemId()
        );
        AutoBid currentAutoBid = new AutoBid(
            request.itemId(),
            bidder,
            request.maxBidLimit(),
            null
        );
        Item item = itemService.getItem(request.itemId());
        
        // Xác thực hạn mức giá tối đa phải đáp ứng các yêu cầu đấu giá cơ bản
        validateBasicBidRequirement(
            item,
            bidder,
            itemStatus,
            request.maxBidLimit()
        );

        // Kiểm tra xem người dùng hiện tại có phải là người đã cài đặt cấu hình Auto-Bid trước đó trên sản phẩm này không
        boolean isSameAutoBidder =
            autoBidOP.isPresent() &&
            autoBidOP.get().getBidder().getUsername().equals(bidderName);

        if (isSameAutoBidder) {
            // Trường hợp người dùng cập nhật lại giới hạn Auto-Bid của chính họ
            AutoBid prevAutoBid = autoBidOP.get();
            double oldMax = prevAutoBid.getMaxBidLimit();
            double newMax = request.maxBidLimit();

            if (newMax > oldMax) {
                // Nếu nâng giới hạn, thực hiện trừ thêm phần chênh lệch tài khoản
                userService.deductBalance(bidderName, newMax - oldMax);
                prevAutoBid.setMaxBidLimit(newMax);
                bidService.saveAutoBid(prevAutoBid);
            } else if (newMax < oldMax) {
                // Nếu hạ giới hạn, hoàn lại tiền thừa phần chênh lệch cho tài khoản
                userService.addBalance(bidderName, oldMax - newMax);
                prevAutoBid.setMaxBidLimit(newMax);
                bidService.saveAutoBid(prevAutoBid);
            }
        } else if (autoBidOP.isPresent()) {
            // Trường hợp có một Auto-Bid của người khác (prevUser) đang hoạt động trên sản phẩm
            AutoBid prevAutoBid = autoBidOP.get();
            User prevUser = prevAutoBid.getBidder();

            // Nếu cấu hình tối đa mới lớn hơn giới hạn Auto-Bid cũ của đối thủ
            if (request.maxBidLimit() > prevAutoBid.getMaxBidLimit()) {
                // Đối thủ thua cuộc: hoàn lại tiền cược cũ của đối thủ, trừ toàn bộ tiền cược tối đa mới của người mới
                userService.addBalance(prevUser.getUsername(), prevAutoBid.getMaxBidLimit());
                userService.deductBalance(bidderName, request.maxBidLimit());
                
                // Thiết lập giá trị cược hiện tại của Auto-Bid mới bắt đầu từ bước giá kế tiếp (nextBidStep)
                currentAutoBid.setCurrentBidValue(itemStatus.getNextBidStep());

                // Cập nhật người dẫn đầu tiếp theo là người dùng mới này
                itemStatus.setNextBidStep(bidderName);

                bidService.saveAutoBid(currentAutoBid);
            } else {
                // Nếu cấu hình tối đa mới nhỏ hơn hoặc bằng giới hạn Auto-Bid cũ của đối thủ
                // Người dùng mới lập tức thất bại. Auto-Bid cũ nâng mức cược hiện tại lên bằng hạn mức của người mới
                prevAutoBid.setCurrentBidValue(request.maxBidLimit());
                
                // Giữ vị trí dẫn đầu cho đối thủ (prevUser) tại mức giá mới này
                updateItemStatusHighestBidder(
                    itemStatus,
                    prevUser.getUsername(),
                    prevAutoBid.getCurrentBidValue()
                );
                bidService.saveAutoBid(prevAutoBid);
            }
            itemStatusService.saveStatus(itemStatus);
        } else {
            // Trường hợp sản phẩm chưa có ai cài đặt Auto-Bid trước đó
            // Hoàn lại tiền cho người dẫn đầu đấu giá thủ công hiện tại
            userService.addBalance(itemStatus.getHighestBidUser(), itemStatus.getCurrentPrice());

            // Bắt đầu cược tự động với mức giá bằng bước giá tối thiểu tiếp theo
            currentAutoBid.setCurrentBidValue(itemStatus.getNextBidStep());
            bidService.saveAutoBid(currentAutoBid);

            // Ghi nhận người đặt Auto-Bid là người sẽ dẫn đầu bước cược tiếp theo
            itemStatus.setNextBidStep(bidderName);
            itemStatusService.saveStatus(itemStatus);

            // Khấu trừ toàn bộ số tiền tối đa đặt Auto-Bid từ ví tài khoản người cài đặt
            userService.deductBalance(bidderName, request.maxBidLimit());
        }
        
        // Cập nhật giá sản phẩm và áp dụng gia hạn giờ chống bắn tỉa (Anti-Sniper)
        itemPricesSink.publishPrice(request.itemId(), itemStatus.getCurrentPrice());
        applyAntiBidExtension(itemStatus);
        return new BaseResponse(true, "succesfully make auto bid");
    }

    /**
     * Kiểm tra các ràng buộc cơ bản của một lượt cược hoặc cấu hình cược tối đa.
     *
     * @param item       Sản phẩm đang được đấu giá
     * @param user       Người dùng đang thực hiện đặt cược
     * @param itemStatus Trạng thái đấu giá hiện tại của sản phẩm
     * @param value      Số tiền cược cần kiểm tra (hoặc hạn mức tối đa của Auto-Bid)
     */
    private void validateBasicBidRequirement(
        Item item,
        User user,
        ItemStatus itemStatus,
        Double value
    ) {
        // Không cho phép người dùng tự đấu giá sản phẩm do chính mình đăng bán
        if (item.getUser().getUsername().equals(user.getUsername())) {
            throw new BaseException("You can't place bid on your own item");
        }
        // Số tiền cược phải lớn hơn giá khởi điểm
        if (itemStatus.getStartingPrice() > value) {
           throw new BaseException("Your bid must be higher than the starting price");
        }
        // Xác thực thời gian phiên đấu giá chưa kết thúc
        validateAuctionNotEnded(item.getItemId());
        // Xác thực người dùng có đủ số dư khả dụng trong ví
        validateUserHaveEnoughMoney(user, value);
        // Xác thực số tiền đặt cược phải vượt trội hơn giá hiện tại + bước giá tối thiểu
        validateHigherThanCurrentPrice(itemStatus, value);
    }

    /**
     * Kiểm tra xem số dư ví của người dùng có đủ để thực hiện lượt cược hay không.
     */
    private void validateUserHaveEnoughMoney(User user, Double value) {
        if (user.getBalance() < value) {
            throw new BaseException("You don't have enough money");
        }
    }

    /**
     * Kiểm tra xem thời gian đấu giá của sản phẩm đã kết thúc chưa.
     */
    private void validateAuctionNotEnded(Long itemId) {
        if (itemStatusService.auctionEndedOrNot(itemId)) {
            throw new BaseException("Auction has already ended");
        }
    }

    /**
     * Cập nhật thông tin người dẫn đầu cao nhất mới và mức giá hiện tại cho sản phẩm.
     */
    private void updateItemStatusHighestBidder(
        ItemStatus itemStatus,
        String username,
        Double bidAmount
    ) {
        itemStatus.setHighestBidUser(username);
        itemStatus.setCurrentPrice(bidAmount);
    }

    /**
     * Xác thực mức đặt cược phải cao hơn mức giá hiện tại ít nhất bằng bước giá quy định (bidIncrement).
     */
    private void validateHigherThanCurrentPrice(
        ItemStatus itemStatus,
        Double value
    ) {
        if (
            itemStatus.getCurrentPrice() + itemStatus.getBidIncrement() > value
        ) {
            throw new BaseException(
                "Your bid must be higher than the current highest"
            );
        }
    }

    /**
     * Cơ chế bù giờ chống bắn tỉa (Anti-Sniper).
     * Nếu thời gian còn lại của phiên đấu giá ít hơn thời gian cấu hình extraTime,
     * đồng thời thời gian kết thúc chưa vượt quá hạn mức tối đa cho phép (maxEndTime),
     * tiến hành gia hạn thêm thời gian kết thúc đúng bằng extraTime tính từ thời điểm đặt cược này.
     */
    private void applyAntiBidExtension(ItemStatus itemStatus) {
        Long remainingTime =
            itemStatus.getEndTime() - Instant.now().toEpochMilli();
        if (
            remainingTime < extraTime &&
            itemStatus.getEndTime() < itemStatus.getMaxEndTime()
        ) {
            itemStatus.setEndTime(Instant.now().toEpochMilli() + extraTime);
        }
        itemStatusService.saveStatus(itemStatus);
    }
}
