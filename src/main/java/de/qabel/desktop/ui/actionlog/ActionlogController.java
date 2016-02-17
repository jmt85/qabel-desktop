package de.qabel.desktop.ui.actionlog;

import de.qabel.core.config.Contact;
import de.qabel.core.config.Identity;
import de.qabel.core.drop.DropMessage;
import de.qabel.core.exceptions.*;
import de.qabel.desktop.config.ClientConfiguration;
import de.qabel.desktop.repository.ContactRepository;
import de.qabel.desktop.repository.DropMessageRepository;
import de.qabel.desktop.repository.exception.EntityNotFoundExcepion;
import de.qabel.desktop.repository.exception.PersistenceException;
import de.qabel.desktop.ui.AbstractController;
import de.qabel.desktop.ui.actionlog.item.ActionlogItem;
import de.qabel.desktop.ui.actionlog.item.ActionlogItemView;
import de.qabel.desktop.ui.actionlog.item.MyActionlogItemView;
import de.qabel.desktop.ui.actionlog.item.OtherActionlogItemView;
import de.qabel.desktop.ui.connector.Connector;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

import javax.inject.Inject;
import java.net.URL;
import java.util.*;

import static java.lang.Thread.*;


public class ActionlogController extends AbstractController implements Initializable, Observer {

	int sleepTime = 60000;
	List<ActionlogItemView> messageView = new LinkedList<>();

	@FXML
	VBox messages;

	@FXML
	TextArea textarea;
	@Inject
	ClientConfiguration clientConfiguration;

	@Inject
	private ContactRepository contactRepository;
	@Inject
	private DropMessageRepository dropMessageRepository;
	@Inject
	Connector httpDropConnector;

	Identity identity;
	Contact c = null;
	List<PersistenceDropMessage> receivedDropMessages;
	List<ActionlogItem> messageControllers = new LinkedList<>();
	Thread dateRefresher;

	public void initialize(URL location, ResourceBundle resources) {

		startThreads();
		identity = clientConfiguration.getSelectedIdentity();
		dropMessageRepository.addObserver(this);
		clientConfiguration.addObserver(this);
		addListener();
	}

	private void startThreads() {
		dateRefresher = new Thread(() -> {
			while (true) {
				messageControllers.forEach(ActionlogItem::refreshDate);
				try {
					sleep(sleepTime);
				} catch (InterruptedException ignored) {
				}
			}
		});
		dateRefresher.start();
	}

	private void addListener() {

		textarea.setOnKeyPressed(keyEvent -> {
			if (keyEvent.getCode().equals(KeyCode.ENTER) && keyEvent.isControlDown()) {
				try {
					handleSubmitButtonAction();
				} catch (QblException | PersistenceException | EntityNotFoundExcepion e) {
					e.printStackTrace();
				}
			}
		});
	}

	@FXML
	protected void handleSubmitButtonAction(ActionEvent event) throws QblDropPayloadSizeException, EntityNotFoundExcepion, PersistenceException, QblDropInvalidMessageSizeException, QblVersionMismatchException, QblSpoofedSenderException, QblNetworkInvalidResponseException {
		handleSubmitButtonAction();
	}

	protected void handleSubmitButtonAction() throws QblDropPayloadSizeException, EntityNotFoundExcepion, PersistenceException, QblDropInvalidMessageSizeException, QblVersionMismatchException, QblSpoofedSenderException, QblNetworkInvalidResponseException {
		if (textarea.getText().equals("") || c == null) {
			return;
		}
		sendDropMessage(c, textarea.getText());
		textarea.setText("");
	}

	void sendDropMessage(Contact c, String text) throws QblDropPayloadSizeException, QblNetworkInvalidResponseException, PersistenceException {
		DropMessage d = new DropMessage(identity, text, "dropMessage");
		dropMessageRepository.addMessage(d, identity, c, true);
		httpDropConnector.send(c, d);
	}

	void loadMessages(Contact c) throws EntityNotFoundExcepion {
		try {
			if (receivedDropMessages == null) {
				messages.getChildren().clear();
				receivedDropMessages = dropMessageRepository.loadConversation(c, identity);
				addMessagesToView(receivedDropMessages);
			} else {
				List<PersistenceDropMessage> newMessages = dropMessageRepository.loadNewMessagesFromConversation(receivedDropMessages, c, identity);
				addNewMessagesToReceivedDropMessages(newMessages);
				addMessagesToView(newMessages);
			}

		} catch (PersistenceException e) {
			e.printStackTrace();
		}
	}

	private void addNewMessagesToReceivedDropMessages(List<PersistenceDropMessage> newMessages) {
		for (PersistenceDropMessage d : newMessages) {
			receivedDropMessages.add(d);
		}
	}

	private void addMessagesToView(List<PersistenceDropMessage> dropMessages) throws EntityNotFoundExcepion {
		for (PersistenceDropMessage d : dropMessages) {

			if (d.getSend()) {
				addOwnMessageToActionlog(d.getDropMessage());
			} else {
				addMessageToActionlog(d.getDropMessage());
			}
		}
	}

	void addMessageToActionlog(DropMessage dropMessage) throws EntityNotFoundExcepion {
		Map<String, Object> injectionContext = new HashMap<>();
		Contact sender = contactRepository.findByKeyId(identity, dropMessage.getSenderKeyId());
		injectionContext.put("dropMessage", dropMessage);
		injectionContext.put("contact", sender);
		OtherActionlogItemView otherItemView = new OtherActionlogItemView(injectionContext::get);
		messages.getChildren().add(otherItemView.getView());
		messageView.add(otherItemView);
		messageControllers.add((ActionlogItem) otherItemView.getPresenter());

	}

	void addOwnMessageToActionlog(DropMessage dropMessage) {

		if (dropMessage.getDropPayload().equals("")) {
			return;
		}
		Map<String, Object> injectionContext = new HashMap<>();
		injectionContext.put("dropMessage", dropMessage);
		MyActionlogItemView myItemView = new MyActionlogItemView(injectionContext::get);
		messages.getChildren().add(myItemView.getView());
		messageView.add(myItemView);
		messageControllers.add((ActionlogItem) myItemView.getPresenter());

	}

	void setText(String text) {
		this.textarea.setText(text);
	}

	@Override
	public void update(Observable o, Object arg) {

		if (o instanceof DropMessageRepository) {
			Platform.runLater(() -> {
				try {
					loadMessages(c);
				} catch (EntityNotFoundExcepion entityNotFoundExcepion) {
					entityNotFoundExcepion.printStackTrace();
				}
			});
		} else if (arg instanceof Identity && o instanceof ClientConfiguration) {
			identity = clientConfiguration.getSelectedIdentity();
		}
	}

	public void setContact(Contact contact) {
		Platform.runLater(() -> {
			try {
				receivedDropMessages = null;
				this.c = contact;
				loadMessages(c);
			} catch (EntityNotFoundExcepion entityNotFoundExcepion) {
				entityNotFoundExcepion.printStackTrace();
			}
		});

	}
}
