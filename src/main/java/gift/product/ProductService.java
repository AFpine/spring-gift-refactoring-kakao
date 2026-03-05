package gift.product;

import gift.category.Category;
import gift.category.CategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public Page<ProductResponse> getProducts(Pageable pageable) {
        return productRepository.findAll(pageable).map(ProductResponse::from);
    }

    public Product getProduct(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + id));
    }

    public Product createProduct(String name, int price, String imageUrl, Long categoryId, boolean allowKakao) {
        validateName(name, allowKakao);
        Category category = findCategory(categoryId);
        return productRepository.save(new Product(name, price, imageUrl, category));
    }

    public Product updateProduct(Long id, String name, int price, String imageUrl, Long categoryId, boolean allowKakao) {
        validateName(name, allowKakao);
        Product product = getProduct(id);
        Category category = findCategory(categoryId);
        product.update(name, price, imageUrl, category);
        return productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public List<String> validateProductName(String name, boolean allowKakao) {
        List<String> errors = new ArrayList<>(Product.checkName(name));
        if (!allowKakao && name != null && name.contains("카카오")) {
            errors.add("\"카카오\"가 포함된 상품명은 담당 MD와 협의한 경우에만 사용할 수 있습니다.");
        }
        return errors;
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
            .orElseThrow(() -> new NoSuchElementException("카테고리가 존재하지 않습니다. id=" + categoryId));
    }

    private void validateName(String name, boolean allowKakao) {
        List<String> errors = validateProductName(name, allowKakao);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }
}
