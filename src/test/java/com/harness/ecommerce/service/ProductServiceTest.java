package com.harness.ecommerce.service;

import com.harness.ecommerce.exception.ResourceNotFoundException;
import com.harness.ecommerce.model.Product;
import com.harness.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("product")
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @InjectMocks private ProductService productService;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    static Product buildProduct(Long id, String name, BigDecimal price, int stock, String category) {
        return Product.builder().id(id).name(name).price(price).stock(stock)
                .category(category).active(true).build();
    }

    static Stream<Product> validProductProvider() {
        return Stream.of(
            buildProduct(1L, "Laptop Pro",      new BigDecimal("1299.99"), 50,  "Electronics"),
            buildProduct(2L, "Wireless Mouse",  new BigDecimal("29.99"),   200, "Electronics"),
            buildProduct(3L, "Standing Desk",   new BigDecimal("499.00"),  15,  "Furniture"),
            buildProduct(4L, "USB-C Hub",       new BigDecimal("49.99"),   100, "Electronics"),
            buildProduct(5L, "Ergonomic Chair", new BigDecimal("389.00"),  25,  "Furniture"),
            buildProduct(6L, "Mechanical Keyboard", new BigDecimal("159.99"), 75, "Electronics"),
            buildProduct(7L, "Monitor 4K",      new BigDecimal("599.99"),  30,  "Electronics"),
            buildProduct(8L, "Desk Lamp",       new BigDecimal("45.00"),   60,  "Accessories"),
            buildProduct(9L, "Webcam HD",       new BigDecimal("89.99"),   80,  "Electronics"),
            buildProduct(10L,"Cable Organizer", new BigDecimal("12.99"),   300, "Accessories")
        );
    }

    // ── Create Product ────────────────────────────────────────────────────────

    @ParameterizedTest(name = "createProduct [{index}] {0}")
    @MethodSource("validProductProvider")
    @DisplayName("Create product - valid products")
    void createProduct_validProduct_returnsSaved(Product product) throws InterruptedException {
        when(productRepository.save(any(Product.class))).thenReturn(product);
        Thread.sleep(30); // simulate I/O latency
        Product result = productService.createProduct(product);
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(product.getName());
        verify(productRepository).save(product);
    }

    // ── Get By ID ─────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "getById [{index}] id={0}")
    @ValueSource(longs = {1, 2, 3, 4, 5, 10, 99, 1000})
    @DisplayName("Get product by ID - found")
    void getById_existingId_returnsProduct(Long id) throws InterruptedException {
        Product product = buildProduct(id, "Test Product", new BigDecimal("10.00"), 5, "Test");
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        Thread.sleep(20);
        Product result = productService.getById(id);
        assertThat(result.getId()).isEqualTo(id);
    }

    @ParameterizedTest(name = "getById_notFound [{index}] id={0}")
    @ValueSource(longs = {999, 1001, 5000, 9999})
    @DisplayName("Get product by ID - not found throws exception")
    void getById_missingId_throwsException(Long id) {
        when(productRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> productService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ── Search ────────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "search [{index}] keyword={0}")
    @ValueSource(strings = {"laptop", "mouse", "desk", "keyboard", "monitor", "lamp"})
    @DisplayName("Search by name keyword - valid")
    void searchByName_validKeyword_returnsList(String keyword) throws InterruptedException {
        Product p = buildProduct(1L, keyword + " Pro", new BigDecimal("99.99"), 10, "Electronics");
        when(productRepository.findByNameContainingIgnoreCase(keyword)).thenReturn(List.of(p));
        Thread.sleep(25);
        List<Product> result = productService.searchByName(keyword);
        assertThat(result).isNotEmpty();
    }

    @ParameterizedTest(name = "search_blank [{index}] keyword=\"{0}\"")
    @ValueSource(strings = {"", "  ", "\t"})
    @DisplayName("Search by name - blank keyword throws exception")
    void searchByName_blankKeyword_throwsException(String keyword) {
        assertThatThrownBy(() -> productService.searchByName(keyword))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── Price Range ───────────────────────────────────────────────────────────

    @ParameterizedTest(name = "priceRange [{index}] min={0}, max={1}")
    @CsvSource({
        "0.01, 50.00",
        "50.00, 200.00",
        "200.00, 500.00",
        "500.00, 1000.00",
        "1000.00, 9999.99",
        "10.00, 10.00"
    })
    @DisplayName("Get by price range - valid ranges")
    void getByPriceRange_validRange_returnsList(String min, String max) throws InterruptedException {
        when(productRepository.findByPriceBetween(any(), any())).thenReturn(List.of());
        Thread.sleep(20);
        List<Product> result = productService.getByPriceRange(new BigDecimal(min), new BigDecimal(max));
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Get by price range - min > max throws exception")
    void getByPriceRange_invertedRange_throwsException() {
        assertThatThrownBy(() -> productService.getByPriceRange(
                new BigDecimal("100.00"), new BigDecimal("50.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── Stock Management ──────────────────────────────────────────────────────

    @ParameterizedTest(name = "updateStock [{index}] delta={0}")
    @CsvSource({
        "1,  10,  11",
        "5,  20,  25",
        "-3, 10,   7",
        "-10, 10,  0",
        "100, 0, 100"
    })
    @DisplayName("Update stock - valid deltas")
    void updateStock_validDelta_updatesCorrectly(int delta, int initial, int expected) throws InterruptedException {
        Product product = buildProduct(1L, "Test", new BigDecimal("10.00"), initial, "Test");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Thread.sleep(20);
        Product result = productService.updateStock(1L, delta);
        assertThat(result.getStock()).isEqualTo(expected);
    }

    @Test
    @DisplayName("Update stock - below zero throws exception")
    void updateStock_belowZero_throwsException() {
        Product product = buildProduct(1L, "Test", new BigDecimal("10.00"), 5, "Test");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        assertThatThrownBy(() -> productService.updateStock(1L, -10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── Category ──────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "getByCategory [{index}] category={0}")
    @ValueSource(strings = {"Electronics", "Furniture", "Accessories", "Books", "Clothing"})
    @DisplayName("Get products by category")
    void getByCategory_returnsFilteredList(String category) throws InterruptedException {
        when(productRepository.findByCategoryAndActiveTrue(category)).thenReturn(List.of());
        Thread.sleep(15);
        List<Product> result = productService.getByCategory(category);
        assertThat(result).isNotNull();
    }

    // ── Deactivate / Activate ─────────────────────────────────────────────────

    @Test
    @DisplayName("Deactivate product - sets active=false")
    void deactivate_existingProduct_setsInactive() {
        Product product = buildProduct(1L, "Old Product", new BigDecimal("10.00"), 5, "Test");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        productService.deactivate(1L);
        assertThat(product.isActive()).isFalse();
    }

    @Test
    @DisplayName("Activate product - sets active=true")
    void activate_inactiveProduct_setsActive() {
        Product product = Product.builder().id(1L).name("Old Product")
                .price(new BigDecimal("10.00")).stock(5).category("Test").active(false).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        productService.activate(1L);
        assertThat(product.isActive()).isTrue();
    }

    @Test
    @DisplayName("Activate product - not found throws exception")
    void activate_missingProduct_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> productService.activate(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Get All (including inactive) ──────────────────────────────────────────

    @Test
    @DisplayName("getAll - returns all products including inactive")
    void getAll_returnsAllProducts() {
        Product active   = buildProduct(1L, "Active Product",   new BigDecimal("10.00"), 5, "Test");
        Product inactive = Product.builder().id(2L).name("Inactive Product")
                .price(new BigDecimal("20.00")).stock(0).category("Test").active(false).build();
        when(productRepository.findAll()).thenReturn(List.of(active, inactive));
        List<Product> result = productService.getAll();
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Product::isActive).containsExactly(true, false);
    }

    @Test
    @DisplayName("isInStock - sufficient stock returns true")
    void isInStock_sufficientStock_returnsTrue() {
        Product product = buildProduct(1L, "Test", new BigDecimal("10.00"), 20, "Test");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        assertThat(productService.isInStock(1L, 10)).isTrue();
    }

    @Test
    @DisplayName("isInStock - insufficient stock returns false")
    void isInStock_insufficientStock_returnsFalse() {
        Product product = buildProduct(1L, "Test", new BigDecimal("10.00"), 5, "Test");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        assertThat(productService.isInStock(1L, 10)).isFalse();
    }
}
