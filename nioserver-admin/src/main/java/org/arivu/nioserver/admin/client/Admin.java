package org.arivu.nioserver.admin.client;

import java.util.ArrayList;
import java.util.Date;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Admin implements EntryPoint {

	private VerticalPanel mainPanel = new VerticalPanel();
	private FlexTable routesFlexTable = new FlexTable();
	private HorizontalPanel addPanel = new HorizontalPanel();
	private TextBox routeNameTextBox = new TextBox();
	private TextBox packageTextBox = new TextBox();
	private Button addRouteButton = new Button("Add");
	private Label lastUpdatedLabel = new Label();
	private ArrayList<RouteData> allRoutes = new ArrayList<RouteData>();
	/**
	 * The message displayed to the user when the server cannot be reached or
	 * returns an error.
	 */
	private static final String SERVER_ERROR = "An error occurred while "
			+ "attempting to contact the server. Please check your network " + "connection and try again.";

	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {
		
		// Add styles to elements in the stock list table.
		routesFlexTable.addStyleName("watchList");
		
		// Routes Table
		routesFlexTable.getRowFormatter().addStyleName(0, "watchListHeader");
		routesFlexTable.setText(0, 0, "Uri");
		routesFlexTable.setText(0, 1, "HttpMethod");
		routesFlexTable.setText(0, 2, "Proxy");
		routesFlexTable.setText(0, 3, "Remove");

		// Assemble Add Route panel.
		addPanel.add(routeNameTextBox);
		addPanel.add(packageTextBox);
		addPanel.add(addRouteButton);
		addPanel.addStyleName("addPanel");
		
		// Assemble Main panel.
		mainPanel.add(routesFlexTable);
		mainPanel.add(addPanel);
		mainPanel.add(lastUpdatedLabel);

		// Associate the Main panel with the HTML host page.
		RootPanel.get("pathList").add(mainPanel);

		// Move cursor focus to the input box.
		routeNameTextBox.setFocus(true);
		
		routeNameTextBox.setTitle("Enter new route name:");
		packageTextBox.setTitle("Enter new package name:");
		addRouteButton.setTitle("Press to add jar file!");
		
		// Listen for mouse events on the Add button.
		addRouteButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				addRoute();
			}
		});

		refreshTable();

	}

	void refreshTable() {
		// Create the popup dialog box
		final DialogBox dialogBox = new DialogBox();
		dialogBox.setText("Remote Procedure Call");
		dialogBox.setAnimationEnabled(true);
		final Button closeButton = new Button("Close");
		// We can set the id of a widget by accessing its Element
		closeButton.getElement().setId("closeButton");
		final Label textToServerLabel = new Label();
		final HTML serverResponseLabel = new HTML();
		VerticalPanel dialogVPanel = new VerticalPanel();
		dialogVPanel.addStyleName("dialogVPanel");
		dialogVPanel.add(new HTML("<b>Sending name to the server:</b>"));
		dialogVPanel.add(textToServerLabel);
		dialogVPanel.add(new HTML("<br><b>Server replies:</b>"));
		dialogVPanel.add(serverResponseLabel);
		dialogVPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
		dialogVPanel.add(closeButton);
		dialogBox.setWidget(dialogVPanel);

		closeButton.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				dialogBox.hide();
			}
		});

		String url = "/__admin/routes";
		RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));

		try {
			// Request request =
			builder.sendRequest(null, new RequestCallback() {
				public void onError(Request request, Throwable exception) {
					// Couldn't connect to server (could be timeout, SOP
					// violation, etc.)
					dialogBox.setText("Server Call - Failure " + exception.toString());
					serverResponseLabel.addStyleName("serverResponseLabelError");
					serverResponseLabel.setHTML(SERVER_ERROR);
					dialogBox.center();
					closeButton.setFocus(true);
				}

				public void onResponseReceived(Request request, Response response) {
					if (200 == response.getStatusCode()) {
						routesFlexTable.clear();
						
						// Routes Table
						routesFlexTable.getRowFormatter().addStyleName(0, "watchListHeader");
						routesFlexTable.setText(0, 0, "Uri");
						routesFlexTable.setText(0, 1, "HttpMethod");
						routesFlexTable.setText(0, 2, "Proxy");
						routesFlexTable.setText(0, 3, "Remove");

						allRoutes.clear();
						
						updateTable(JsonUtils.<JsArray<RouteData>>safeEval(response.getText()));
						
					} else {
						// Handle the error. Can get the status text from
						// response.getStatusText()
						dialogBox.setText("Remote Procedure Call - Failure " + response.getStatusText());
						serverResponseLabel.addStyleName("serverResponseLabelError");
						serverResponseLabel.setHTML(SERVER_ERROR);
						dialogBox.center();
						closeButton.setFocus(true);
					}
				}
			});
		} catch (RequestException e) {
			// Couldn't connect to server
		}
	}

	@SuppressWarnings("deprecation")
	void updateTable(JsArray<RouteData> routes) {
		allRoutes.clear();
		for (int i = 0; i < routes.length(); i++) {
			final RouteData routeData = routes.get(i);
			final int row = routesFlexTable.getRowCount();
			allRoutes.add(routeData);
			routesFlexTable.setText(row, 0, routeData.getUri());
			routesFlexTable.setText(row, 1, routeData.getMethod());
			routesFlexTable.setText(row, 2, routeData.getProxy());
			Button removeRouteButton = new Button("x");
			removeRouteButton.addStyleDependentName("remove");
		    removeRouteButton.addClickHandler(new ClickHandler() {
		      public void onClick(ClickEvent event) {
		        int removedIndex = row;
		        allRoutes.remove(routeData);
		        routesFlexTable.removeRow(removedIndex);
		        removeRoute( routeData );
		      }
		    });
			routesFlexTable.setWidget(row, 3, removeRouteButton);
			routesFlexTable.getCellFormatter().addStyleName(row, 0, "watchListFirstColumn");
			routesFlexTable.getCellFormatter().addStyleName(row, 1, "watchListColumn");
			routesFlexTable.getCellFormatter().addStyleName(row, 2, "watchListColumn");
			routesFlexTable.getCellFormatter().addStyleName(row, 3, "watchListRemoveColumn");
		}

		// Display timestamp showing last refresh.
		lastUpdatedLabel.setText("Last update : " + DateTimeFormat.getMediumDateTimeFormat().format(new Date()));

		// Clear any errors.
		// errorMsgLabel.setVisible(false);

	}

	protected void removeRoute(RouteData routeData) {
		
	}

	protected void addRoute() {

	}
}

class RouteData extends JavaScriptObject {
	protected RouteData() {
	}

	// JSNI methods to get route data.
	public final native String getUri() /*-{ return this.uri; }-*/;

	public final native String getMethod() /*-{ return this.method; }-*/;

	public final native String getProxy() /*-{ return this.proxy; }-*/;
}
