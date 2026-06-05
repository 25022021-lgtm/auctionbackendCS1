package com.auction.bids;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.auction.items.Item;
import com.auction.users.User;

/**
 * Repository cung cấp các phương thức thao tác cơ sở dữ liệu với thực thể Bid.
 */
@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    /**
     * Tìm kiếm một lượt đặt giá cụ thể theo người dùng và mặt hàng.
     */
    Optional<Bid> findByUserAndItem(User user, Item item);

    /**
     * Tìm kiếm phân trang tất cả các lượt đặt giá của một người dùng.
     */
    Page<Bid> findAllByUser(User user, Pageable pageable);

    /**
     * Kiểm tra người dùng đã đặt giá cược cho sản phẩm này hay chưa.
     */
    boolean existsByUserAndItem(User user, Item item);

    /**
     * Kiểm tra xem sản phẩm này đã từng được ai đặt giá cược hay chưa.
     */
    boolean existsByItem(Item item);

    /**
     * Lấy phân trang lịch sử đặt cược của một sản phẩm cụ thể.
     * Sử dụng JPQL Custom Query kết hợp JOIN FETCH b.item để tránh lỗi N+1 query.
     *
     * @param pageable Cấu hình phân trang và sắp xếp
     * @param itemId   Mã ID sản phẩm
     * @return Danh sách phân trang các lượt cược
     */
    @Query(value = "SELECT b FROM Bid b JOIN FETCH b.item WHERE b.item.itemId = :itemId")
    public Page<Bid> findItemBidHistory(Pageable pageable, @Param("itemId") Long itemId);

    /**
     * Truy vấn gốc (Native Query) tìm các lượt đấu giá chiến thắng của người dùng.
     * Một người thắng cuộc khi:
     * - Họ đặt giá cho sản phẩm (`bids.bidder_username = :username`)
     * - Họ giữ giá cao nhất hiện tại ở bảng trạng thái (`item_statuses.username = :username`)
     * - Thời gian đấu giá của sản phẩm đó đã trôi qua (`item_statuses.end_time < :now`)
     *
     * @param username Tên đăng nhập người dùng cần kiểm tra
     * @param now      Thời điểm hiện tại dưới dạng Epoch Milliseconds để đối chiếu thời gian kết thúc
     * @return Danh sách các lượt đặt giá giành chiến thắng
     */
    @NativeQuery(value = "SELECT bids.* FROM bids INNER JOIN items ON bids.item_id = items.item_id INNER JOIN item_statuses ON items.item_id = item_statuses.item_id WHERE :username = bids.bidder_username AND item_statuses.username = :username AND item_statuses.end_time < :now")
    public List<Bid> getWinsByUser(@Param("username") String username, @Param("now") Long now);

    /**
     * Xóa các lượt đặt giá dựa trên thông tin sản phẩm và người dùng.
     */
    @Transactional
    Long deleteByItemAndUser(Item item, User user);
}
