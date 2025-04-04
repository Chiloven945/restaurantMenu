package chiloven.menu;

import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.stage.FileChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MenuAppController {

    private static final Logger logger = LogManager.getLogger(MenuAppController.class);
    private final List<String> orderHistory = new ArrayList<>();
    private final Map<String, Spinner<Integer>> foodQuantityMap = new HashMap<>();
    private final List<FoodCategory> loadedCategories = new ArrayList<>();
    private final Map<String, Integer> categoryLimits = new HashMap<>();
    private final Map<String, List<String>> categoryItems = new HashMap<>();
    private final Map<String, Label> categoryRemainingLabels = new HashMap<>();
    @FXML
    private VBox menuContainer;
    @FXML
    private TextArea orderHistoryArea;

    @FXML
    private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Menu App");
        alert.setContentText("Version 1.0\nDeveloped by Mackenzie\n© 2025");
        alert.showAndWait();
    }

    @FXML
    private void handleSettings() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Settings");
        dialog.setHeaderText("Application Settings");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().add(new Label("(Settings UI not implemented yet.)"));

        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    @FXML
    private void handleExport() {
        if (orderHistory.isEmpty()) {
            logger.warn("Order was empty!");
            showAlert("Export", "No orders to export.");
            return;
        }
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Order History");
            fileChooser.setInitialFileName("order_history.txt");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                    new FileChooser.ExtensionFilter("CSV Files", "*.csv")
            );
            File file = fileChooser.showSaveDialog(null);

            if (file != null) try (PrintWriter writer = new PrintWriter(file)) {
                boolean isCsv = file.getName().endsWith(".csv");
                if (isCsv) {
                    logger.info("User chose the csv format.");
                    writer.println("Timestamp,Item,Quantity");
                    for (String order : orderHistory) {
                        String[] lines = order.split("\n");
                        String timestamp = lines[0];
                        Arrays.stream(lines, 1, lines.length)
                                .filter(line -> line.contains(" x "))
                                .map(line -> line.split(" x ", 2))
                                .forEach(parts -> writer.printf("\"%s\",\"%s\",%s%n", timestamp, parts[0], parts[1]));
                    }
                } else {
                    logger.info("User chose the txt format.");
                    for (String order : orderHistory) writer.println(order + "\n");
                }
                showAlert("Export", "Order history exported successfully to:\n" + file.getAbsolutePath());
            }
        } catch (Exception ex) {
            logger.error("Failed to export order history", ex);
            showAlert("Export Error", "An error occurred while exporting.");
        }
    }

    @FXML
    public void initialize() {
        List<FoodCategory> foodCategories = loadFoodCategories();
        loadedCategories.addAll(foodCategories);

        for (FoodCategory categoryData : foodCategories) {
            String category = categoryData.category;
            List<MenuItemData> items = categoryData.items;

            int limit = categoryData.limit != null ? categoryData.limit : 8;

            categoryLimits.put(category, limit);
            categoryItems.put(category, new ArrayList<>());

            try {
                FXMLLoader titleLoader = new FXMLLoader(getClass().getResource("/category_title.fxml"));
                VBox titleBox = titleLoader.load();
                CategoryTitleController titleController = titleLoader.getController();
                titleController.setTitle(category);
                menuContainer.getChildren().add(titleBox);

                Label remainingLabel = new Label();
                remainingLabel.setStyle("-fx-text-fill: #999999; -fx-font-size: 12px;");
                categoryRemainingLabels.put(category, remainingLabel);
                menuContainer.getChildren().add(remainingLabel);
            } catch (Exception e) {
                logger.error("Failed to load category title FXML", e);
            }

            for (MenuItemData item : items) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/menu_item.fxml"));
                    HBox itemBox = loader.load();
                    MenuItemController controller = loader.getController();

                    String imagePath = (item.image != null && !item.image.isEmpty()) ? item.image : "images/placeholder.png";
                    InputStream imageStream = getClass().getResourceAsStream("/" + imagePath);
                    if (imageStream == null) {
                        logger.warn("Image not found: {}, using fallback.", imagePath);
                        imageStream = getClass().getResourceAsStream("/images/placeholder.png");
                    }
                    Image image = new Image(imageStream);

                    controller.setItemData(item.name, item.description, item.price, image);

                    Spinner<Integer> spinner = new Spinner<>(0, limit, 0);
                    spinner.setEditable(true);
                    controller.quantitySpinner.setValueFactory(spinner.getValueFactory());

                    controller.quantitySpinner.valueProperty().addListener((obs, oldVal, newVal) -> updateCategoryLimits(category));
                    controller.remainingLabel.setText("剩余可选：" + limit);

                    foodQuantityMap.put(item.name, controller.quantitySpinner);
                    categoryItems.get(category).add(item.name);

                    controller.quantitySpinner.valueProperty().addListener((obs, oldVal, newVal) -> updateCategoryLimits(category));

                    menuContainer.getChildren().add(itemBox);
                } catch (Exception e) {
                    logger.error("Failed to load menu item FXML", e);
                }
            }
        }
    }

    @FXML
    private void placeOrder() {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        String timestamp = sdf.format(new Date());
        StringBuilder order = new StringBuilder(timestamp + "\nOrder:");
        boolean hasOrder = false;

        for (Map.Entry<String, Spinner<Integer>> entry : foodQuantityMap.entrySet()) {
            int quantity = entry.getValue().getValue();
            if (quantity > 0) {
                order.append("\n").append(entry.getKey()).append(" x ").append(quantity);
                hasOrder = true;
            }
        }

        if (hasOrder) {
            orderHistory.add(order.toString());
            updateOrderHistory();
            showAlert("Notification", "Order placed successfully!");
            clearSelection();
            logger.info("New order placed: {}", order);
        }
    }

    private void updateOrderHistory() {
        orderHistoryArea.setText("Order History:\n");
        for (String order : orderHistory) {
            orderHistoryArea.appendText(order + "\n\n");
        }
        logger.info("Order history updated.");
    }

    private void clearSelection() {
        for (Spinner<Integer> spinner : foodQuantityMap.values()) {
            spinner.getValueFactory().setValue(0);
        }
        logger.info("Selection cleared after placing order.");
    }

    private List<FoodCategory> loadFoodCategories() {
        try (InputStream is = getClass().getResourceAsStream("/food_menu.json")) {
            if (is == null) throw new RuntimeException("food_menu.json not found");
            Reader reader = new InputStreamReader(is);
            Gson gson = new Gson();
            FoodCategory[] categories = gson.fromJson(reader, FoodCategory[].class);
            return Arrays.asList(categories);
        } catch (Exception e) {
            logger.error("Failed to load food categories", e);
            showAlert("Error", "Unable to load menu data.");
            return new ArrayList<>();
        }
    }

    private void updateCategoryLimits(String category) {
        int total = 0;
        for (String item : categoryItems.get(category)) {
            total += foodQuantityMap.get(item).getValue();
        }
        int remaining = categoryLimits.get(category) - total;

        Label remainingLabel = categoryRemainingLabels.get(category);
        if (remainingLabel != null) {
            remainingLabel.setText("Remaining: " + remaining);
        }
        for (String item : categoryItems.get(category)) {
            Spinner<Integer> spinner = foodQuantityMap.get(item);
            int current = spinner.getValue();
            spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Math.max(current + remaining, current), current));

            Node parent = spinner.getParent();
            if (parent instanceof VBox vbox) {
                for (Node child : vbox.getChildren()) {
                    if (child instanceof Label label && "remainingLabel".equals(label.getId())) {
                        label.setText("剩余可选：" + remaining);
                    }
                }
            }
        }
    }

    private void showAlert(String title, String message) {
        logger.info("Alert called.\n{}: {}", title, message);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class MenuItemData {
        public String name;
        public String description;
        public String price;
        public String image;
    }

    public static class FoodCategory {
        public String category;
        public List<MenuItemData> items;
        public Integer limit;
    }
}
