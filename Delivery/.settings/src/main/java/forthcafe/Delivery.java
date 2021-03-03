package forthcafe;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;

import forthcafe.external.Coupon;
import forthcafe.external.CouponService;

import java.util.List;

@Entity
@Table(name="Delivery_table")
public class Delivery {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String ordererName;
    private String menuName;
    private Long menuId;
    private Double price;
    private Integer quantity;
    private String status;


    @PrePersist
    public void onPrePersist(){
        Deliveried deliveried = new Deliveried();
        BeanUtils.copyProperties(this, deliveried);
        deliveried.setStatus("deliveried");
        // kafka push
        deliveried..publish();

        // req/res 패턴 처리 
        Coupon coupon = new Coupon();
        BeanUtils.copyProperties(this, coupon);
        // feignclient 호출
        DeliveryApplication.applicationContext.getBean(CouponService.class).coupon(coupon);
    }
    }

    @PostUpdate
    public void onPostUpdate(){
        DeliveryCancelled deliveryCancelled = new DeliveryCancelled();
        BeanUtils.copyProperties(this, deliveryCancelled);
        deliveryCancelled.setStatus("deliveryCancelled");
        deliveryCancelled.publishAfterCommit();

    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }
    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOrdererName() {
        return ordererName;
    }

    public void setOrdererName(String ordererName) {
        this.ordererName = ordererName;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public Long getMenuId() {
        return menuId;
    }

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }
}
