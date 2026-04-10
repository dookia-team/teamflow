# 백엔드 Java 코드 패턴 참고 자료 (코드 예시 전용)

> **이 파일은 코드 예시만 포함합니다. 규칙·컨벤션은 `backend-conventions.md`를 참고하세요.**
> 디자인 패턴 가이드: `backend-design-patterns.md`

---

## 레이어별 코드 구조 (product 도메인 예시)

### dto/ProductDto.java — DTO inner record 선언

```java
// ── product/dto/ProductDto.java ────────────────────────────────────────────
public class ProductDto {

    public record CreateRequest(
        @NotBlank(message = "상품명은 필수입니다") @Size(max = 255) String name,
        @NotNull @Positive BigDecimal price,
        @NotNull Long categoryId
    ) {}

    public record UpdateRequest(
        @Size(max = 255) String name,
        @Positive BigDecimal price
    ) {}

    public record Response(
        Long id,
        String name,
        BigDecimal price,
        String status
    ) {
        public static Response from(Product product) {
            return new Response(
                product.getId(),
                product.getName(),
                product.getBasePrice(),
                product.getStatus().name()
            );
        }
    }
}
```

### controller/ProductController.java

```java
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto.Response>> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductDto.Response>> createProduct(
        @RequestBody @Valid ProductDto.CreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(productService.create(request)));
    }
}
```

### service/ProductService.java — 인터페이스 (다형성 필요 시)

```java
public interface ProductService {
    ProductDto.Response getById(Long id);
    ProductDto.Response create(ProductDto.CreateRequest request);
}
```

### service/impl/ProductServiceImpl.java — 비즈니스 로직

```java
@Service @RequiredArgsConstructor @Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public ProductDto.Response getById(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
        return ProductDto.Response.from(product);
    }

    @Override
    public ProductDto.Response create(ProductDto.CreateRequest request) {
        Product product = Product.builder()
            .name(request.name())
            .basePrice(request.price())
            .categoryId(request.categoryId())
            .status(ProductStatus.DRAFT)
            .build();
        return ProductDto.Response.from(productRepository.save(product));
    }
}
```

### repository/ProductRepository.java

```java
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryIdAndStatus(Long categoryId, ProductStatus status);
    Page<Product> findByBrandIdOrderByCreatedAtDesc(Long brandId, Pageable pageable);
}
```
