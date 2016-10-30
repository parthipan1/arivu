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
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
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
	private TextBox uriTextBox = new TextBox();
	private TextBox locationTextBox = new TextBox();
	private ListBox dropDownList = new ListBox();
	private Button addRouteButton = new Button("Add");
	private Label lastUpdatedLabel = new Label();
	
	private ListBox appsDropdownList = new ListBox();
	
	private ArrayList<Data> allRoutes = new ArrayList<Data>();
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
		routesFlexTable.setText(0, 0, "Name");
		routesFlexTable.setText(0, 1, "Uri");
		routesFlexTable.setText(0, 2, "HttpMethod");
		routesFlexTable.setText(0, 3, "Proxy");
		routesFlexTable.setText(0, 4, "Remove");

		// Assemble Add Route panel.
		addPanel.add(routeNameTextBox);
		addPanel.add(uriTextBox);
		addPanel.add(locationTextBox);
		addPanel.add(dropDownList);
		addPanel.add(addRouteButton);
		addPanel.addStyleName("addPanel");

		// Assemble Main panel.
		mainPanel.add(routesFlexTable);
		mainPanel.add(addPanel);
		mainPanel.add(lastUpdatedLabel);
		
		addDeployPanel();
		addUnDeployPanel();
		
		// Associate the Main panel with the HTML host page.
		RootPanel.get("pathList").add(mainPanel);

		// Move cursor focus to the input box.
		routeNameTextBox.setFocus(true);

		routeNameTextBox.setTitle("Enter new route name:");
		uriTextBox.setTitle("Enter new uri name:");
		locationTextBox.setTitle("Enter new location name:");
		addRouteButton.setTitle("Press to add route!");
		
		dropDownList.addItem("browser");
		dropDownList.addItem("proxy");

		// Listen for mouse events on the Add button.
		addRouteButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				addRoute();
			}
		});

		refreshTable();
	}

	
	void addUnDeployPanel(){
		VerticalPanel panel = new VerticalPanel();
		panel.add(appsDropdownList);
		Button undeployButton = new Button("Undeploy");
		undeployButton.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				String selectedValue = appsDropdownList.getSelectedValue();
				if( !isNullOrEmpty(selectedValue) )
					undeployApp(selectedValue);
			}
		});
		
		refreshAppsList();
		panel.add(undeployButton);
		
		mainPanel.add(panel);
	}

	void undeployApp(String name) {
		String url = "/__admin/undeploy?name="+name;
		RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));

		try {
			builder.sendRequest(null, new RequestCallback() {
				public void onError(Request request, Throwable exception) {
					alertWidget("Failed on apps", "Error from server :: "+exception.toString());
				}

				public void onResponseReceived(Request request, Response response) {
					if (200 == response.getStatusCode()) {
						refreshAppsList();
					} else {
						alertWidget("Failed on apps", "Error from server :: "+response.getText());
					}
				}
			});
		} catch (RequestException e) {
			// Couldn't connect to server
		}
	}
	
	void refreshAppsList() {
		String url = "/__admin/apps";
		RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));

		try {
			builder.sendRequest(null, new RequestCallback() {
				public void onError(Request request, Throwable exception) {
					alertWidget("Failed on apps", "Error from server :: "+exception.toString());
				}

				public void onResponseReceived(Request request, Response response) {
					if (200 == response.getStatusCode()) {
						for(int i=0;i<appsDropdownList.getItemCount();i++){
							appsDropdownList.removeItem(i);
						}
						JsArray<Data> routes = JsonUtils.<JsArray<Data>>safeEval(response.getText());
						for (int i = 0; i < routes.length(); i++) {
							appsDropdownList.addItem(routes.get(i).getName());
						}
					} else {
						alertWidget("Failed on apps", "Error from server :: "+response.getText());
					}
				}
			});
		} catch (RequestException e) {
			// Couldn't connect to server
		}
	}
	
	void addDeployPanel(){
	    VerticalPanel panel = new VerticalPanel();
	      //create a FormPanel 
	      final FormPanel form = new FormPanel();
	      //create a file upload widget
	      final FileUpload fileUpload = new FileUpload();
	      //create labels
	      Label selectLabel = new Label("Select a file:");
	      //create upload button
	      Button uploadButton = new Button("Upload App");
	      //pass action to the form to point to service handling file 
	      //receiving operation.
	      form.setAction("/__admin/deploy");
	      // set form to use the POST method, and multipart MIME encoding.
	      form.setEncoding(FormPanel.ENCODING_MULTIPART);
	      form.setMethod(FormPanel.METHOD_POST);
	    
	      fileUpload.setName("dist");
	      
	      TextBox appNameTextBox = new TextBox();
	  	  TextBox appPackageTextBox = new TextBox();
	      
	  	  appNameTextBox.setTitle("Name of the App");
	  	  appNameTextBox.setName("name");
	  	  appPackageTextBox.setTitle("Packages to scan");
	  	  appPackageTextBox.setName("scanpackages");
	  	  
	  	  panel.add(appNameTextBox);
	  	  panel.add(appPackageTextBox);
	  	  
	      //add a label
	      panel.add(selectLabel);
	      //add fileUpload widget
	      panel.add(fileUpload);
	      
//	      form.
	      //add a button to upload the file
	      panel.add(uploadButton);
	      uploadButton.addClickHandler(new ClickHandler() {
	         @Override
	         public void onClick(ClickEvent event) {
	            //get the filename to be uploaded
	            String filename = fileUpload.getFilename();
	            if (filename.length() == 0) {
	               Window.alert("No File Specified!");
	            } else {
	               //submit the form
	               form.submit();			          
	            }				
	         }
	      });
	   
	      form.addSubmitCompleteHandler(new FormPanel.SubmitCompleteHandler() {
	         @Override
	         public void onSubmitComplete(SubmitCompleteEvent event) {
	        	 refreshTable();
	        	 refreshAppsList();
	         }
	      });
	      panel.setSpacing(10);
		  
	      // Add form to the root panel.      
	      form.add(panel);
	      
	      mainPanel.add(form);
	}
	
	void refreshTable() {
		routeRequest(RequestBuilder.GET,null);
	}

	@SuppressWarnings("deprecation")
	void updateTable(JsArray<Data> routes) {
		
		for (int i = 0; i < allRoutes.size(); i++) {
			routesFlexTable.removeRow(i+1);
		}
		allRoutes.clear();
		
		for (int i = 0; i < routes.length(); i++) {
			final Data data = routes.get(i);
			final int row = 1+i;
			allRoutes.add(data);
			routesFlexTable.setText(row, 0, data.getName());
			routesFlexTable.setText(row, 1, data.getUri());
			routesFlexTable.setText(row, 2, data.getMethod());
			routesFlexTable.setText(row, 3, data.getProxy());
			
			String active = data.getActive();
			if( "true".equals(active) ){
				Button removeRouteButton = new Button("x");
				removeRouteButton.addStyleDependentName("remove");
				removeRouteButton.addClickHandler(new ClickHandler() {
					public void onClick(ClickEvent event) {
						disable(data);
					}
				});
				routesFlexTable.setWidget(row, 4, removeRouteButton);
			}else{
				Button removeRouteButton = new Button("+");
				removeRouteButton.addStyleDependentName("remove");
				removeRouteButton.addClickHandler(new ClickHandler() {
					public void onClick(ClickEvent event) {
						enable(data);
					}
				});
				routesFlexTable.setWidget(row, 4, removeRouteButton);
			}
			routesFlexTable.getCellFormatter().addStyleName(row, 0, "watchListColumn");
			routesFlexTable.getCellFormatter().addStyleName(row, 1, "watchListFirstColumn");
			routesFlexTable.getCellFormatter().addStyleName(row, 2, "watchListColumn");
			routesFlexTable.getCellFormatter().addStyleName(row, 3, "watchListColumn");
			routesFlexTable.getCellFormatter().addStyleName(row, 4, "watchListRemoveColumn");
		}

//		mainPanel.remove(routesFlexTable);
//		mainPanel.add(routesFlexTable);
		// Display timestamp showing last refresh.
		lastUpdatedLabel.setText("Last update : " + DateTimeFormat.getMediumDateTimeFormat().format(new Date()));

		
		// Clear any errors.
		// errorMsgLabel.setVisible(false);

	}

	boolean isNullOrEmpty(final String v) {
	    return !(v != null && v.length() > 0);
	  }
	
	protected void removeRoute(Data route) {
		disable(route);
	}
	
	DialogBox alertWidget(final String header, final String content) {
        final DialogBox box = new DialogBox();
        final VerticalPanel panel = new VerticalPanel();
        box.setText(header);
        panel.add(new Label(content));
        final Button buttonClose = new Button("Close",new ClickHandler() {
            @Override
            public void onClick(final ClickEvent event) {
                box.hide();
            }
        });
        // few empty labels to make widget larger
        final Label emptyLabel = new Label("");
        emptyLabel.setSize("auto","25px");
        panel.add(emptyLabel);
        panel.add(emptyLabel);
        buttonClose.setWidth("90px");
        panel.add(buttonClose);
        panel.setCellHorizontalAlignment(buttonClose, HasAlignment.ALIGN_RIGHT);
        box.add(panel);
        return box;
    }
	
	protected void addRoute() {
		String uri = uriTextBox.getText();
		String name = routeNameTextBox.getText();
		String loc = locationTextBox.getText();
		String typeRoute = dropDownList.getSelectedValue();
		if( isNullOrEmpty(uri) ){
			alertWidget("Adding route failed",
	                "Invalid uri :: "+uri).center();
			return;
		}
		if( isNullOrEmpty(name) ){
			alertWidget("Adding route failed",
	                "Invalid name :: "+name).center();
			return;
		}
		if( isNullOrEmpty(loc) ){
			alertWidget("Adding route failed",
	                "Invalid loc :: "+loc).center();
			return;
		}
		if( isNullOrEmpty(typeRoute) ){
			alertWidget("Adding route failed",
	                "Invalid route type :: "+typeRoute).center();
			return;
		}
		routeRequest(RequestBuilder.POST,"{\"uri\":\"" + uri + "\",\"name\":\"" + name + "\",\"loc\":\"" + loc + "\",\"type\":\"" + typeRoute + "\"}");
	}

	protected void disable(Data route) {
		routeRequest(RequestBuilder.PUT,"{\"uri\":\"" + route.getUri() + "\",\"method\":\"" + route.getMethod() + "\",\"active\":\"false\"}");
	}

	protected void enable(Data route) {
		routeRequest(RequestBuilder.PUT,"{\"uri\":\"" + route.getUri() + "\",\"method\":\"" + route.getMethod() + "\",\"active\":\"true\"}");
	}

	protected void routeRequest(RequestBuilder.Method method,String body) {
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
		RequestBuilder builder = new RequestBuilder(method, URL.encode(url));

		try {
			// Request request =
			builder.sendRequest(body, new RequestCallback() {
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
						routesFlexTable.setText(0, 0, "Name");
						routesFlexTable.setText(0, 1, "Uri");
						routesFlexTable.setText(0, 2, "HttpMethod");
						routesFlexTable.setText(0, 3, "Proxy");
						routesFlexTable.setText(0, 4, "Remove");

						allRoutes.clear();

						updateTable(JsonUtils.<JsArray<Data>>safeEval(response.getText()));

					} else {
						// Handle the error. Can get the status text from
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
}

class Data extends JavaScriptObject {
	protected Data() {
	}

	// JSNI methods to get route data.
	public final native String getUri() /*-{
		return this.uri;
	}-*/;

	public final native String getMethod() /*-{
		return this.method;
	}-*/;

	public final native String getProxy() /*-{
		return this.proxy;
	}-*/;

	public final native String getActive() /*-{
		return this.active;
	}-*/;

	public final native String getName() /*-{
		return this.name;
	}-*/;
}
