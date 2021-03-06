package de.qabel.desktop.ui.contact.item;


import de.qabel.core.config.Contact;
import de.qabel.desktop.config.ClientConfiguration;
import de.qabel.desktop.repository.IdentityRepository;
import de.qabel.desktop.ui.AbstractController;
import de.qabel.desktop.ui.accounting.avatar.AvatarView;
import de.qabel.desktop.ui.accounting.item.SelectionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

import javax.inject.Inject;
import javax.swing.event.ListSelectionEvent;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class ContactItemController extends AbstractController implements Initializable {

	ResourceBundle resourceBundle;

	@FXML
	Label alias;
	@FXML
	Label email;
	@FXML
	Pane avatarContainer;
	@FXML
	HBox contactRootItem;

	List<Consumer> consumers = new LinkedList<>();

	@Inject
	private Contact contact;
	@Inject
	private ClientConfiguration clientConfiguration;
	@Inject
	private IdentityRepository identityRepository;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		contactRootItem.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
			for (Consumer c : consumers){
				SelectionEvent selectionEvent = new SelectionEvent();
				selectionEvent.setContact(contact);
				selectionEvent.setController(ContactItemController.this);
				c.accept(selectionEvent);
			}
		});
		this.resourceBundle = resources;
		alias.setText(contact.getAlias());
		email.setText(contact.getEmail());
		updateAvatar();
	}

	public void addSelectionListener(Consumer<SelectionEvent> consumer){
		consumers.add(consumer);
	}
	public void select(){
		contactRootItem.getStyleClass().add("selected");
	}

	private void updateAvatar() {
			new AvatarView(e -> contact.getAlias()).getViewAsync(avatarContainer.getChildren()::setAll);
	}

	public void unselect() {
		contactRootItem.getStyleClass().remove("selected");
	}
}
