package com.auction.items;

import com.auction.users.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Entity representing an auction item. */
@Entity
@Table(name = "items")
public class Item {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "item_id")
  private Long itemId;

  @JsonIgnore
  @ManyToOne // One seller, many Items
  @JoinColumn(name = "seller_username") // JoinColumn annotation creates a foreign key column
  private User user;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "description")
  private String description;

  public Item() {}

  public Item(User user, String title, String description) {
    this.user = user;
    this.title = title;
    this.description = description;
  }

  public Long getItemId() {
    return itemId;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Item)) {
      return false;
    }
    Item item = (Item) o;
    return itemId != null && itemId.equals(item.itemId);
  }

  @Override
  public int hashCode() {
    return itemId != null ? itemId.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "Item{"
        + "itemId=" + itemId
        + ", title='" + title + '\''
        + ", description='" + description + '\''
        + '}';
  }
}
