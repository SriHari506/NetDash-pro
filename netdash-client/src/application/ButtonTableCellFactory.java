package application;

import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

public class ButtonTableCellFactory implements Callback<TableColumn<Object, Void>, TableCell<Object, Void>> {

    @Override
    public TableCell<Object, Void> call(final TableColumn<Object, Void> param) {
        return new TableCell<>() {
            private final Button editBtn = new Button("✏️");
            private final Button deleteBtn = new Button("🗑️");
            private final HBox pane = new HBox(10, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("edit-button");
                deleteBtn.getStyleClass().add("delete-button");

                editBtn.setOnAction(e -> {
                    Object device = getTableView().getItems().get(getIndex());
                    // TODO: handle edit logic
                    System.out.println("Edit clicked: " + device);
                });

                deleteBtn.setOnAction(e -> {
                    Object device = getTableView().getItems().get(getIndex());
                    // TODO: handle delete logic
                    System.out.println("Delete clicked: " + device);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        };
    }
}
