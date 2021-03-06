package forthcafe;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;


import java.util.List;

@Entity
@Table(name="Coupon_table")
public class Coupon {

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
                // configMap 설정
        String sysEnv = System.getenv("SYS_MODE");
        if(sysEnv == null) sysEnv = "LOCAL";
        System.out.println("################## SYSTEM MODE: " + sysEnv);

        CouponSaved couponSaved = new CouponSaved();
        BeanUtils.copyProperties(this, couponSaved);
        couponSaved.publishAfterCommit();

                // delay test시 주석해제
        try {
                Thread.currentThread().sleep((long) (400 + Math.random() * 220));
        } catch (InterruptedException e) {
                e.printStackTrace();
        }

    }


    @PostUpdate
    public void onPostUpdate(){
        CouponCancelled couponCancelled = new CouponCancelled();
        BeanUtils.copyProperties(this, couponCancelled);
        couponCancelled.publishAfterCommit();

    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

}
