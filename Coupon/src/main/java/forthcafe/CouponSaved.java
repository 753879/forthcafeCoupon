
package forthcafe;

public class CouponSaved extends AbstractEvent {

    private Long id;
    private Integer piece;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Integer getPiece() {
        return piece;
    }

    public void setPiece(Integer piece) {
        this.piece = piece;
    }
}
