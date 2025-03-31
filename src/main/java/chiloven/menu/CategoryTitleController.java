package chiloven.menu;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class CategoryTitleController {
    @FXML
    private Label titleLabel;

    public void setTitle(String title) {
        titleLabel.setText(title);
    }
}
