package gift.option;

import gift.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Entity
@Table(name = "options")
public class Option {
    private static final int MAX_NAME_LENGTH = 50;
    private static final Pattern ALLOWED_NAME_PATTERN =
        Pattern.compile("^[a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ ()\\[\\]+\\-&/_]*$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private int quantity;

    protected Option() {
    }

    public Option(Product product, String name, int quantity) {
        validateName(name);
        this.product = product;
        this.name = name;
        this.quantity = quantity;
    }

    private static void validateName(String name) {
        List<String> errors = new ArrayList<>();
        if (name == null || name.isBlank()) {
            errors.add("옵션 이름은 필수입니다.");
        } else {
            if (name.length() > MAX_NAME_LENGTH) {
                errors.add("옵션 이름은 공백을 포함하여 최대 50자까지 입력할 수 있습니다.");
            }
            if (!ALLOWED_NAME_PATTERN.matcher(name).matches()) {
                errors.add("옵션 이름에 허용되지 않는 특수 문자가 포함되어 있습니다. 사용 가능: ( ), [ ], +, -, &, /, _");
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }

    public void subtractQuantity(int amount) {
        if (amount > this.quantity) {
            throw new IllegalArgumentException("차감할 수량이 현재 재고보다 많습니다.");
        }
        this.quantity -= amount;
    }

    public int calculatePrice(int quantity) {
        return this.product.getPrice() * quantity;
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }
}
