package org.arivu.nioserver.admin.client;

import java.util.ArrayList;
import java.util.Date;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Admin implements EntryPoint {

	private static final String HASH_HEADER = "X-HASH";
	private static final String H1_ARIVU_NIO_SERVER_H1 = "<H1>Arivu NIO Server</H1>";//
	private VerticalPanel mainPanel = new VerticalPanel();
	private ScrollPanel scrollTablePanel = new ScrollPanel();
	private FlexTable routesFlexTable = new FlexTable();
	private HorizontalPanel addTitlePanel = new HorizontalPanel();
	private HorizontalPanel addPanel = new HorizontalPanel();
	private TextBox routeNameTextBox = new TextBox();
	private TextBox uriTextBox = new TextBox();
	private TextBox locationTextBox = new TextBox();
	private ListBox dropDownList = new ListBox();
	private Button addRouteButton = new Button("Add");
//	private Label lastUpdatedLabel = new Label();
	
	private ListBox appsDropdownList = new ListBox();
	
	private ArrayList<Data> allRoutes = new ArrayList<Data>();
//	/**
//	 * The message displayed to the user when the server cannot be reached or
//	 * returns an error.
//	 */
//	private static final String SERVER_ERROR = "An error occurred while "
//			+ "attempting to contact the server. Please check your network " + "connection and try again.";

	
	private DockLayoutPanel p = new DockLayoutPanel(Unit.EM);
	private HorizontalPanel headerPanel = new HorizontalPanel();
	private HorizontalPanel footerPanel = new HorizontalPanel();
	private VerticalPanel naviPanel = new VerticalPanel();
	private FlowPanel contentPanel = new FlowPanel();
	private Image iconImage = new Image("images/arivu.jpeg");
	private Image stopImage = new Image("images/stop.png");
	private Label titleLabel = new Label();
	
	private long loadTime = 0;
	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {
		loadTime = new Date().getTime();
		headerPanel.add(iconImage);
		headerPanel.add(titleLabel);
		headerPanel.add(stopImage);
		headerPanel.setStyleName("orangeOutline");
		
		titleLabel.getElement().setInnerHTML(H1_ARIVU_NIO_SERVER_H1);
		titleLabel.getElement().setAttribute("style", "text-align: center;vertical-align: top;");
		footerPanel.add(new HTML("High performance NIO Server"));
		footerPanel.setStyleName("orangeOutline");
		
		naviPanel.setStyleName("redOutline");
		contentPanel.setStyleName("blueOutline");
		
		p.setStyleName("greenOutline");

		p.addNorth(headerPanel, 5);
		p.addSouth(footerPanel, 5);
//		p.addWest(naviPanel, 6);
		p.add(contentPanel);
		
		RootLayoutPanel rp = RootLayoutPanel.get();
	    rp.add(p);
		
		init();
	}


	void init() {
		// Add styles to elements in the stock list table.
		routesFlexTable.addStyleName("watchList");
		scrollTablePanel.addStyleName("watchList");
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

		addTitlePanel.add(new Label("Enter new route name:"));
		addTitlePanel.add(new Label("Enter new uri name:"));
		addTitlePanel.add(new Label("Enter new location name:"));
		addTitlePanel.add(new Label("Enter type ofproxy:"));
		addTitlePanel.add(new Label(" "));
		
		// Assemble Main panel.
		scrollTablePanel.add(routesFlexTable);
		mainPanel.add(scrollTablePanel);
		mainPanel.add(addTitlePanel);
		mainPanel.add(addPanel);
		mainPanel.addStyleName("orangeOutline"); 
		
		addDeployPanel();
		addUnDeployPanel();
		
		// Associate the Main panel with the HTML host page.
//		RootPanel.get("pathList").add(mainPanel);
		
		contentPanel.add(mainPanel);
		// Move cursor focus to the input box.
		routeNameTextBox.setFocus(true);

//		routeNameTextBox.setTitle("Enter new route name:");
//		uriTextBox.setTitle("Enter new uri name:");
//		locationTextBox.setTitle("Enter new location name:");
//		addRouteButton.setTitle("Press to add route!");
		
		dropDownList.addItem("browser");
		dropDownList.addItem("proxy");

		// Listen for mouse events on the Add button.
		addRouteButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				addRoute();
			}
		});

		refreshRoutesTable();
	}

	HorizontalPanel undeployPanel = new HorizontalPanel();
	
	void addUnDeployPanel(){
		undeployPanel.add(new Label("Undeploy Artifact :"));
		undeployPanel.add(appsDropdownList);
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
		undeployPanel.add(undeployButton);

		undeployPanel.setStyleName("addPanel");
		mainPanel.add(undeployPanel);
	}

	void undeployApp(String name) {
		String url = "/__admin/undeploy?name="+name;
		RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
		builder.setHeader(HASH_HEADER, String.valueOf(loadTime) );
		try {
			builder.sendRequest(null, new RequestCallback() {
				public void onError(Request request, Throwable exception) {
					alertWidget("Failed on apps", "Error from server :: "+exception.toString());
				}

				public void onResponseReceived(Request request, Response response) {
					if (200 == response.getStatusCode()) {
						refreshRoutesTable();
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
		builder.setHeader(HASH_HEADER, String.valueOf(loadTime) );
		try {
			builder.sendRequest(null, new RequestCallback() {
				public void onError(Request request, Throwable exception) {
					alertWidget("Failed on apps", "Error from server :: "+exception.toString());
				}

				public void onResponseReceived(Request request, Response response) {
					if (200 == response.getStatusCode()) {
						appsDropdownList.clear();
						for(int i=0;i<appsDropdownList.getItemCount();i++){
							appsDropdownList.removeItem(0);
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
	
	HorizontalPanel deployPanel = new HorizontalPanel();
	void addDeployPanel(){
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
	      fileUpload.getElement().setAttribute("accept", ".zip");
	      
	      TextBox appNameTextBox = new TextBox();
	  	  TextBox appPackageTextBox = new TextBox();
	      
	  	  appNameTextBox.setTitle("Name of the App");
	  	  appNameTextBox.setName("name");
	  	  appPackageTextBox.setTitle("Packages to scan");
	  	  appPackageTextBox.setName("scanpackages");
	  	  
	  	  deployPanel.setStyleName("addPanel");
	  	  deployPanel.add(appNameTextBox);
	  	  deployPanel.add(appPackageTextBox);
	  	  
		  Hidden hiddenField = new Hidden();
		  hiddenField.setName( HASH_HEADER );
		  hiddenField.setValue( String.valueOf(loadTime) );
//		  form.add( field );
		  deployPanel.add(hiddenField);
		  
	      //add a label
	      deployPanel.add(selectLabel);
	      //add fileUpload widget
	      deployPanel.add(fileUpload);
	      
//	      form.
	      //add a button to upload the file
	      deployPanel.add(uploadButton);
	      uploadButton.addClickHandler(new ClickHandler() {
	         @Override
	         public void onClick(ClickEvent event) {
	            //get the filename to be uploaded
	            String filename = fileUpload.getFilename();
	            if (isNullOrEmpty(filename)) {
	               alertWidget("Upload ",
	   	                "No files selected for upload!").center();
	            } else {
	               //submit the form
	               form.submit();			          
	            }				
	         }
	      });
	   
	      form.addSubmitCompleteHandler(new FormPanel.SubmitCompleteHandler() {
	         @Override
	         public void onSubmitComplete(SubmitCompleteEvent event) {
	        	 form.reset();
	        	 refreshRoutesTable();
	        	 refreshAppsList();
	         }
	      });
	      deployPanel.setSpacing(10);
		  
	      // Add form to the root panel.      
	      form.add(deployPanel);
	      
	      mainPanel.add(form);
	}
	
	void refreshRoutesTable() {
		routeRequest(RequestBuilder.GET,null);
	}

	void updateTable(JsArray<Data> routes) {
		
		for (int i = allRoutes.size()-1; i > 1; i--) {
			routesFlexTable.removeRow(1);
		}
		allRoutes.clear();
		
		routesFlexTable.getRowFormatter().addStyleName(0, "watchListHeader");
		routesFlexTable.setText(0, 0, "Name");
		routesFlexTable.setText(0, 1, "Uri");
		routesFlexTable.setText(0, 2, "HttpMethod");
		routesFlexTable.setText(0, 3, "Proxy");
		routesFlexTable.setText(0, 4, "Remove");
		
		for (int i = 0; i < routes.length(); i++) {
			final Data data = routes.get(i);
			final int row = 1+i;
			allRoutes.add(data);
			routesFlexTable.setWidget(row, 0, new Label(data.getName()));
			
			String active = data.getActive();
			if( "true".equals(active) ){
				routesFlexTable.setWidget(row, 1, new Anchor(data.getUri(),data.getUri()));
				routesFlexTable.setWidget(row, 2, new Label(data.getMethod()));
				routesFlexTable.setWidget(row, 3, new Label(data.getProxy()));
				Button removeRouteButton = new Button("x");
				removeRouteButton.addStyleDependentName("remove");
				removeRouteButton.addClickHandler(new ClickHandler() {
					public void onClick(ClickEvent event) {
						disable(data);
					}
				});
				routesFlexTable.setWidget(row, 4, removeRouteButton);
			}else{
				routesFlexTable.setWidget(row, 1, new Label(data.getUri()));
				routesFlexTable.setWidget(row, 2, new Label(data.getMethod()));
				routesFlexTable.setWidget(row, 3, new Label(data.getProxy()));
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
		String url = "/__admin/routes";
		RequestBuilder builder = new RequestBuilder(method, URL.encode(url));
		builder.setHeader(HASH_HEADER, String.valueOf(loadTime) );
		try {
			// Request request =
			builder.sendRequest(body, new RequestCallback() {
				public void onError(Request request, Throwable exception) {
					alertWidget("Server error",
			                "Error :: "+exception.toString()).center();
				}

				public void onResponseReceived(Request request, Response response) {
					if ( 200 == response.getStatusCode() || 201 == response.getStatusCode() ) {
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
						alertWidget("Server error",
				                "Error :: "+response.getStatusText()).center();
					}
				}
			});
		} catch (RequestException e) {
			alertWidget("Server error url "+url,
	                "Error :: "+e.toString()).center();
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
