package com.harness.ecommerce.integration;

import com.harness.ecommerce.exception.InsufficientStockException;
import com.harness.ecommerce.model.Order;
import com.harness.ecommerce.model.Product;
import com.harness.ecommerce.model.User;
import com.harness.ecommerce.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@Tag("integration")
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Checkout Integration Tests")
class CheckoutIntegrationTest {

    @Autowired private ProductService productService;
    @Autowired private UserService userService;
    @Autowired private CartService cartService;
    @Autowired private OrderService orderService;

    // ── Full Checkout Flow ────────────────────────────────────────────────────

    @Test
    @DisplayName("Full checkout flow - single item order succeeds")
    void fullCheckout_singleItem_createsOrder() throws InterruptedException {
        User user = userService.createUser(User.builder()
                .name("Integration User").email("int1@test.com")
                .password("Password1!").build());
        Product product = productService.createProduct(Product.builder()
                .name("Integration Product").price(new BigDecimal("29.99"))
                .stock(100).category("Test").build());

        Thread.sleep(50);
        cartService.addItem(user.getId(), product.getId(), 3);
        Order order = orderService.checkout(user.getId());

        assertThat(order.getId()).isNotNull();
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getTotal()).isEqualByComparingTo(new BigDecimal("89.97"));
        assertThat(order.getStatus()).isEqualTo(Order.Status.PENDING);
    }

    @Test
    @DisplayName("Full checkout flow - cart is cleared after checkout")
    void fullCheckout_cartClearedAfterCheckout() throws InterruptedException {
        User user = userService.createUser(User.builder()
                .name("Cart Clear User").email("cartclear@test.com")
                .password("Password1!").build());
        Product product = productService.createProduct(Product.builder()
                .name("Cart Clear Product").price(new BigDecimal("10.00"))
                .stock(50).category("Test").build());

        Thread.sleep(40);
        cartService.addItem(user.getId(), product.getId(), 1);
        orderService.checkout(user.getId());

        int itemCount = cartService.getItemCount(user.getId());
        assertThat(itemCount).isZero();
    }

    @Test
    @DisplayName("Full checkout flow - stock decremented after checkout")
    void fullCheckout_stockDecrementedCorrectly() throws InterruptedException {
        User user = userService.createUser(User.builder()
                .name("Stock User").email("stock@test.com")
                .password("Password1!").build());
        Product product = productService.createProduct(Product.builder()
                .name("Stock Product").price(new BigDecimal("15.00"))
                .stock(20).category("Test").build());

        Thread.sleep(40);
        cartService.addItem(user.getId(), product.getId(), 5);
        orderService.checkout(user.getId());

        Product updated = productService.getById(product.getId());
        assertThat(updated.getStock()).isEqualTo(15);
    }

    @Test
    @DisplayName("Full checkout flow - multiple items calculates total correctly")
    void fullCheckout_multipleItems_correctTotal() throws InterruptedException {
        User user = userService.createUser(User.builder()
                .name("Multi Item User").email("multi@test.com")
                .password("Password1!").build());
        Product p1 = productService.createProduct(Product.builder()
                .name("Item A").price(new BigDecimal("10.00")).stock(50).category("Test").build());
        Product p2 = productService.createProduct(Product.builder()
                .name("Item B").price(new BigDecimal("25.00")).stock(50).category("Test").build());

        Thread.sleep(50);
        cartService.addItem(user.getId(), p1.getId(), 2);  // 20.00
        cartService.addItem(user.getId(), p2.getId(), 1);  // 25.00

        Order order = orderService.checkout(user.getId());
        assertThat(order.getTotal()).isEqualByComparingTo(new BigDecimal("45.00"));
    }

    // ── Parameterized Integration Scenarios ──────────────────────────────────

    @ParameterizedTest(name = "scenario [{index}] qty={0}, price={1}, expected={2}")
    @CsvSource({
        "1,  10.00,  10.00",
        "2,  10.00,  20.00",
        "5,  9.99,   49.95",
        "10, 4.99,   49.90",
        "3,  33.33,  99.99"
    })
    @DisplayName("Checkout total calculation - parameterized scenarios")
    void checkout_totalCalculations_areCorrect(int qty, String price, String expectedTotal)
            throws InterruptedException {
        String email = "param_" + qty + "_" + price.replace(".", "") + "@test.com";
        User user = userService.createUser(User.builder()
                .name("Param User").email(email).password("Password1!").build());
        Product product = productService.createProduct(Product.builder()
                .name("Param Product").price(new BigDecimal(price))
                .stock(100).category("Test").build());

        Thread.sleep(40);
        cartService.addItem(user.getId(), product.getId(), qty);
        Order order = orderService.checkout(user.getId());

        assertThat(order.getTotal()).isEqualByComparingTo(new BigDecimal(expectedTotal));
    }

    // ── Order Lifecycle ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Order lifecycle - PENDING → CONFIRMED → SHIPPED → DELIVERED")
    void orderLifecycle_fullProgression_succeeds() throws InterruptedException {
        User user = userService.createUser(User.builder()
                .name("Lifecycle User").email("lifecycle@test.com")
                .password("Password1!").build());
        Product product = productService.createProduct(Product.builder()
                .name("Lifecycle Product").price(new BigDecimal("50.00"))
                .stock(10).category("Test").build());

        Thread.sleep(50);
        cartService.addItem(user.getId(), product.getId(), 1);
        Order order = orderService.checkout(user.getId());

        Thread.sleep(30);
        order = orderService.updateStatus(order.getId(), Order.Status.CONFIRMED);
        assertThat(order.getStatus()).isEqualTo(Order.Status.CONFIRMED);

        order = orderService.updateStatus(order.getId(), Order.Status.SHIPPED);
        assertThat(order.getStatus()).isEqualTo(Order.Status.SHIPPED);

        order = orderService.updateStatus(order.getId(), Order.Status.DELIVERED);
        assertThat(order.getStatus()).isEqualTo(Order.Status.DELIVERED);
    }

    @Test
    @DisplayName("Cancel order - stock is restored after cancellation")
    void cancelOrder_stockRestored() throws InterruptedException {
        User user = userService.createUser(User.builder()
                .name("Cancel User").email("cancel@test.com")
                .password("Password1!").build());
        Product product = productService.createProduct(Product.builder()
                .name("Cancel Product").price(new BigDecimal("20.00"))
                .stock(10).category("Test").build());

        Thread.sleep(50);
        cartService.addItem(user.getId(), product.getId(), 4);
        Order order = orderService.checkout(user.getId());

        int stockAfterCheckout = productService.getById(product.getId()).getStock();
        assertThat(stockAfterCheckout).isEqualTo(6);

        Thread.sleep(30);
        orderService.cancelOrder(order.getId());
        int stockAfterCancel = productService.getById(product.getId()).getStock();
        assertThat(stockAfterCancel).isEqualTo(10);
    }

    @Test
    @DisplayName("Out of stock - checkout fails when stock exhausted")
    void checkout_outOfStock_throwsException() throws InterruptedException {
        User user = userService.createUser(User.builder()
                .name("OOS User").email("oos@test.com")
                .password("Password1!").build());
        Product product = productService.createProduct(Product.builder()
                .name("OOS Product").price(new BigDecimal("5.00"))
                .stock(2).category("Test").build());

        Thread.sleep(40);
        assertThatThrownBy(() -> cartService.addItem(user.getId(), product.getId(), 10))
                .isInstanceOf(InsufficientStockException.class);
    }
}
