package chiloven.menu;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class MenuItemController {
    @FXML
    public Label itemName;
    @FXML
    public Label itemDescription;
    @FXML
    public Label itemPrice;
    @FXML
    public Spinner<Integer> quantitySpinner;
    @FXML
    public ImageView itemImage;

    public void setItemData(String name, String description, String price, Image image) {
        itemName.setText(name);
        itemDescription.setText(description);
        itemPrice.setText(price);
        itemImage.setImage(image);
    }
}
