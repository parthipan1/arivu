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
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PushButton;
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
	
	private DockLayoutPanel p = new DockLayoutPanel(Unit.EM);
	private HorizontalPanel headerPanel = new HorizontalPanel();
	private HorizontalPanel footerPanel = new HorizontalPanel();
	private VerticalPanel naviPanel = new VerticalPanel();
	private Image iconImage = new Image("images/arivu.jpeg");
	private PushButton stopButton = new PushButton(new Image("images/stop.png"));
	private Label titleLabel = new Label();
	private Label footerLabel = new Label("High performance NIO Server");
	
	private static String[] proxyTypes = {"browser","proxy"};
	
	private long loadTime = 0;
	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {
		loadTime = new Date().getTime();
		
		stopButton.setHeight("60px");
		stopButton.setWidth("60px");
		stopButton.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				shutdownServer();
			}
		});
		stopButton.setTitle("Stop the server!");
		
		headerPanel.add(iconImage);
		headerPanel.add(titleLabel);
		headerPanel.add(stopButton);
		headerPanel.setStyleName("headerAndFooter");
		
		titleLabel.getElement().setInnerHTML(H1_ARIVU_NIO_SERVER_H1);
		titleLabel.getElement().setAttribute("style", "text-align: center;vertical-align: top;");
		footerPanel.add(footerLabel);
		footerPanel.setStyleName("headerAndFooter");
		
//		footerLabel.getElement().setInnerHTML();
		
		naviPanel.setStyleName("naviOutline");
		mainPanel.setStyleName("mainOutline");
		
		p.setStyleName("contentOutline");

		p.addNorth(headerPanel, 10);
		p.addSouth(footerPanel, 3);
		p.addWest(naviPanel, 1);
		p.add(mainPanel);
		
		RootLayoutPanel rp = RootLayoutPanel.get();
	    rp.add(p);
		
		init();
	}

	private HorizontalPanel routesTableTitlePanel = new HorizontalPanel();
	private HTML routesTableTitleLabel = new HTML("<h4>&nbsp;List of all Routes Registered on server:</h4>");
	private HTML newRouteLabel = new HTML("&nbsp;Route:");
	private Label newUriLabel = new Label("Uri:");
	private Label newLocationLabel = new Label("Location:");
	private Label newProxyLabel = new Label("Proxy:");
	private HTML addRouteLabel = new HTML("<h4>&nbsp;Add New Route:</h4>");

	void init() {
		routesTableTitlePanel.add(routesTableTitleLabel);
		mainPanel.add(routesTableTitlePanel);
		// Add styles to elements in the stock list table.
		routesFlexTable.addStyleName("watchList");
		scrollTablePanel.addStyleName("watchList");
//		// Routes Table

		scrollTablePanel.setHeight("150px");
		
		// Assemble Add Route panel.

		addPanel.add(newRouteLabel);
		addPanel.add(routeNameTextBox);
		addPanel.add(newUriLabel);
		addPanel.add(uriTextBox);
		addPanel.add(newLocationLabel);
		addPanel.add(locationTextBox);
		addPanel.add(newProxyLabel);
		addPanel.add(dropDownList);
		addPanel.add(addRouteButton);
		addPanel.addStyleName("addPanel");

		
		addTitlePanel.add(addRouteLabel);
		
		// Assemble Main panel.
		scrollTablePanel.add(routesFlexTable);
		mainPanel.add(scrollTablePanel);
		mainPanel.add(addTitlePanel);
		mainPanel.add(addPanel);
//		mainPanel.addStyleName("headerAndFooter"); 
		
		addDeployPanel();
		addUnDeployPanel();
		
		// Move cursor focus to the input box.
		routeNameTextBox.setFocus(true);

		for(String s:proxyTypes)
			dropDownList.addItem(s);

		// Listen for mouse events on the Add button.
		addRouteButton.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				addRoute();
			}
		});

		refreshRoutesTable();
	}

	private HorizontalPanel deployTitlePanel = new HorizontalPanel();
	private HorizontalPanel deployPanel = new HorizontalPanel();
	FormPanel fileUploadFormPanel = new FormPanel();
	FileUpload fileUpload = new FileUpload();
	Button uploadButton = new Button("Upload");
	TextBox appNameTextBox = new TextBox();
	TextBox appPackageTextBox = new TextBox();
	Label appNameLabel = new Label(" App Name:");
	Label packagesLabel = new Label("Packages:");
	Hidden hiddenField = new Hidden();
	HTML deployTitleLabel = new HTML("<h4>&nbsp;Upload New App:</h4>");
	
	void addDeployPanel(){
		  deployTitlePanel.add(deployTitleLabel);
		  mainPanel.add(deployTitlePanel);
	      //receiving operation.
	      fileUploadFormPanel.setAction("/__admin/deploy");
	      // set form to use the POST method, and multipart MIME encoding.
	      fileUploadFormPanel.setEncoding(FormPanel.ENCODING_MULTIPART);
	      fileUploadFormPanel.setMethod(FormPanel.METHOD_POST);
	      fileUpload.setName("dist");
	      fileUpload.getElement().setAttribute("accept", ".zip");
	      
	      
	  	  appNameTextBox.setTitle("Name of the App");
	  	  appNameTextBox.setName("name");
	  	  appPackageTextBox.setTitle("comma seperated packages to scan for Paths");
	  	  appPackageTextBox.setName("scanpackages");
	  	  
	  	  deployPanel.setStyleName("addPanel");
	  	  
	  	  
		  deployPanel.add(appNameLabel);
	  	  deployPanel.add(appNameTextBox);
		  deployPanel.add(packagesLabel);
	  	  deployPanel.add(appPackageTextBox);
	  	  
		  hiddenField.setName( HASH_HEADER );
		  hiddenField.setValue( String.valueOf(loadTime) );
		  deployPanel.add(hiddenField);
		  
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
	               fileUploadFormPanel.submit();			          
	            }				
	         }
	      });
	   
	      fileUploadFormPanel.addSubmitCompleteHandler(new FormPanel.SubmitCompleteHandler() {
	         @Override
	         public void onSubmitComplete(SubmitCompleteEvent event) {
	        	 fileUploadFormPanel.reset();
	        	 refreshRoutesTable();
	        	 refreshAppsList();
	         }
	      });
	      deployPanel.setSpacing(10);
		  
	      // Add form to the root panel.      
	      fileUploadFormPanel.add(deployPanel);
	      
	      mainPanel.add(fileUploadFormPanel);
	}
	
	private HorizontalPanel undeployTitlePanel = new HorizontalPanel();
	private HorizontalPanel undeployPanel = new HorizontalPanel();
	private HTML removeAppLabel = new HTML("<h4>&nbsp;Remove App:</h4>");
	private HTML selectAppLabel = new HTML("&nbsp;Select an App :");
	private Button undeployButton = new Button("Undeploy");
	
	void addUnDeployPanel(){
		
		undeployTitlePanel.add(removeAppLabel);
		mainPanel.add(undeployTitlePanel);
		undeployPanel.add(selectAppLabel);
		undeployPanel.add(appsDropdownList);
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
	
	void refreshRoutesTable() {
		routeRequest(RequestBuilder.GET,null);
	}

	boolean isNullOrEmpty(final String v) {
	    return !(v != null && v.length() > 0);
	  }
	
	protected void removeRoute(Data route) {
		disable(route);
	}
	
	void addRoute() {
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

	void disable(Data route) {
		routeRequest(RequestBuilder.PUT,"{\"uri\":\"" + route.getUri() + "\",\"method\":\"" + route.getMethod() + "\",\"active\":\"false\"}");
	}

	void enable(Data route) {
		routeRequest(RequestBuilder.PUT,"{\"uri\":\"" + route.getUri() + "\",\"method\":\"" + route.getMethod() + "\",\"active\":\"true\"}");
	}

	void routeRequest(final RequestBuilder.Method method,String body) {
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
						updateTable(JsonUtils.<JsArray<Data>>safeEval(response.getText()));
						if( method == RequestBuilder.POST ){
							routeNameTextBox.setText("");
							uriTextBox.setText("");
							locationTextBox.setText("");
							dropDownList.setItemSelected(0, true);
						}
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
			alertWidget("Server request failed!", "Error :: "+e.getMessage());
		}
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
			alertWidget("Server request failed!", "Error :: "+e.getMessage());
		}
	}

	void shutdownServer() {
		String url = "/__admin/shutdown";
		RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, URL.encode(url));
		builder.setHeader(HASH_HEADER, String.valueOf(loadTime) );
		try {
			builder.sendRequest(null, new RequestCallback() {
				public void onError(Request request, Throwable exception) {
					alertWidget("Failed on apps", "Error from server :: "+exception.toString());
				}

				public void onResponseReceived(Request request, Response response) {
					if (200 == response.getStatusCode()) {
						alertWidget("Server shutdown!", "Server is down!");
					} else {
						alertWidget("Failed on apps", "Error from server :: "+response.getText());
					}
				}
			});
		} catch (RequestException e) {
			alertWidget("Server request failed!", "Error :: "+e.getMessage());
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
