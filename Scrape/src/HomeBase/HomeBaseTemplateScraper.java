package HomeBase;

import java.awt.TextArea;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import BNQ.BNQMultiScraper;
import BNQ.EDA;
import BNQ.EDANewFunctions;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.application.Application.Parameters;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class HomeBaseTemplateScraper extends Application {


	ConcurrentHashMap<Integer,Integer> tabWebPos = new ConcurrentHashMap<Integer,Integer>();
	ConcurrentHashMap<Integer,Integer> page = new ConcurrentHashMap<Integer,Integer>();
	ConcurrentHashMap<Integer,Integer> poLookedAt = new ConcurrentHashMap<Integer,Integer>();
	ConcurrentHashMap<Integer,Integer> amountToLookAt = new ConcurrentHashMap<Integer,Integer>();
	ConcurrentHashMap<Integer,Boolean> finished = new ConcurrentHashMap<Integer,Boolean>();
	ConcurrentHashMap<Integer,String> holdPO = new ConcurrentHashMap<Integer,String>();



	int lastPage =1;
	int lastPlace = 1;
	int seqNo = 1;
	CopyOnWriteArrayList<StatusReport> listOfReports = new CopyOnWriteArrayList<StatusReport>();

	@Override
	public void start(Stage primaryStage) throws Exception {

		Parameters p = getParameters();
		List<String> s = p.getRaw();
		lastPage = Integer.parseInt(s.get(0));
		lastPlace = Integer.parseInt(s.get(1)) - 1;
		TabPane tabPane = new TabPane();
		for(int i = 0;i <10;i++)
		{
			Tab tab1 = new Tab();
			tab1.setText("browser" + i);
			tab1.setContent(makeBrowserTab("browser" + i,i));
			tabPane.getTabs().add(tab1);
			page.put(i, 1);
			poLookedAt.put(i, 0);
			if(lastPlace < i)
			{
				amountToLookAt.put(i,(lastPage-1));
			}
			else 
			{
				amountToLookAt.put(i,lastPage);
			}
			if(amountToLookAt.get(i) == 0)
			{
				tabWebPos.put(i, 8);
				finished.put(i, true);

			}
			else
			{
				tabWebPos.put(i, 1);
				finished.put(i, false);

			}



		}



		Scene scene = new Scene(tabPane);
		primaryStage.setTitle("Two browsers");
		primaryStage.setScene(scene);
		primaryStage.show();

		Task task = new Task(){

			@Override
			protected Object call() throws Exception {

				while(true)
				{
					boolean ready = true;
					for(Integer i : poLookedAt.keySet()) 
					{
						if(finished.get(i) == false )
						{
							ready = false;
						}
					}
					if(ready)
					{
						Collections.sort(listOfReports, new Comparator<StatusReport>() {

							//cant compare full po remove first number
							@Override
							public int compare(StatusReport o1, StatusReport o2) {
								try {
									return Integer.parseInt(o2.orderNumber.substring(1, o2.orderNumber.length())) - Integer.parseInt(o1.orderNumber.substring(1, o1.orderNumber.length())) ;
								} catch (NumberFormatException e) {
									System.out.println(e.getMessage());
									return 0;
								}
							}

						});
						System.out.println(listOfReports.size() + " status updates adding");
						StatusReport.createStatusReport(listOfReports);
						System.out.println(listOfReports.size() + " status updates after");
						Platform.runLater(() -> {
							Alert alert = new Alert(Alert.AlertType.INFORMATION, "Closing Now");
							alert.setHeaderText("Program Finished");
							alert.showAndWait();
							System.exit(0);
						});
						
						return null;
					}
				}

			}

		};
		Thread th = new Thread(task);
		th.setDaemon(true);
		System.out.println("Starting background task...");
		th.start();


	}

	private Node makeBrowserTab(String identifier,int place) {

		BorderPane borderPane = new BorderPane();
		HBox hbox = new HBox();
		WebView browser = new WebView();
		browser.setPrefSize(960, 640);
		borderPane.setTop(hbox);
		borderPane.setCenter(browser);

		WebEngine engine = browser.getEngine();


		engine.load("https://gxstradeweb.gxsolc.com/pub-log/login.html?lang=en");

		engine.getLoadWorker().stateProperty().addListener(
				new ChangeListener<State>() {
					public void changed(ObservableValue ov, State oldState, State newState) {

						if( newState != Worker.State.SUCCEEDED ) {
							return;
						}

						if(tabWebPos.get(place) == 1)
						{
							try {
								engine.executeScript("form.User.value='***REMOVED***'; form.Password.value='***REMOVED***';form.submit.click();");
								tabWebPos.put(place, 3);
							} catch (Exception e) {

								e.printStackTrace();
							}
						}

						//click inbox
						else if(tabWebPos.get(place) ==3)
						{

							String html = (String) engine.executeScript("document.documentElement.outerHTML");
							if(isError(html))
							{
								tabWebPos.put(place, 1);
								engine.load("https://gxstradeweb.gxsolc.com/pub-log/login.html?lang=en");
							}

							try {
								engine.load("https://gxstradeweb.gxsolc.com/edi-bin/EdiMailbox.pl?NextDocListed=Next&LastStartNum=" + (((page.get(place) -2)*10)+1) + "&box_type=in&lang=en&sort_var=");

								tabWebPos.put(place,4);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						}
						//go to your place
						else if(tabWebPos.get(place) ==4)
						{			

							String html = (String) engine.executeScript("document.documentElement.outerHTML");
							if(isError(html))
							{
								System.out.println("eror");
								tabWebPos.put(place, 1);
								engine.load("https://gxstradeweb.gxsolc.com/pub-log/login.html?lang=en");
							}
							try {


								engine.executeScript("window.open('https://gxstradeweb.gxsolc.com' + form.ReadUrl" + place + ".value,'_self')");



								tabWebPos.put(place,5);
							} catch (Exception e) {
							}
						}
						//go to inner html doc
						else if(tabWebPos.get(place) ==5)
						{

							try
							{
								engine.executeScript("window.open(MAINAREA.location,'_self')");
								tabWebPos.put(place,6);

							} catch (Exception e) {
							}

						}
						else if(tabWebPos.get(place) ==6)
						{
						
							try
							{
								
								String html = (String) engine.executeScript("document.documentElement.outerHTML");
								String po = getPo(html);
								String link = findInvoiceLink(html);
								holdPO.put(place, po);
								engine.load("https://gxstradeweb.gxsolc.com/" + link);	
								tabWebPos.put(place,7);

							} catch (Exception e) {
								System.out.println("here");
								poLookedAt.put(place, poLookedAt.get(place)+1);
								page.put(place, page.get(place) + 1);
								System.out.println("looked at " + poLookedAt.get(place));
								System.out.println("need to look at " + amountToLookAt.get(place));
								engine.load("https://gxstradeweb.gxsolc.com/edi-bin/EdiMailbox.pl?NextDocListed=Next&LastStartNum=" + (((page.get(place) -2)*10)+1) + "&box_type=in&lang=en&sort_var=");
								tabWebPos.put(place,4);
							}

						}
						else if(tabWebPos.get(place) ==7)
						{
							try
							{
								engine.executeScript("var templateName = "+holdPO.get(place)+";var edi_sub = parent.MAINAREA.document.Form1.EDI_SUB.value;var edi_sub_args1 = parent.MAINAREA.document.Form1.EDI_SUB_ARGS1.value;var edi_sub_args2 = parent.MAINAREA.document.Form1.EDI_SUB_ARGS2.value;var edi_sub_com=\"parent.MAINAREA.\" + edi_sub + \"(parent.MAINAREA.\" + edi_sub_args1 + \",parent.MAINAREA.document.\" + edi_sub_args2 + \")\";parent.MAINAREA.document.Form1.Save.value=2;if (eval (edi_sub_com)){parent.MAINAREA.document.Form1.SaveFile2.value=templateName;parent.MAINAREA.document.Form1.SaveFile1.value=\" \";parent.MAINAREA.document.Form1.target=\"_parent\";parent.MAINAREA.document.Form1.submit();};");
								poLookedAt.put(place, poLookedAt.get(place)+1);
								page.put(place, page.get(place) + 1);
								System.out.println("looked at " + poLookedAt.get(place));
								System.out.println("need to look at " + amountToLookAt.get(place));
								
								
								tabWebPos.put(place,3);

							} catch (Exception e) {
							}
						}

					}


				});



		return borderPane;
	}

	public boolean isError(String html)
	{
		return html.contains("GXS TradeWeb Error Message");
	}
	public String getPo(String html)
	{
		String orderNumber;
		orderNumber = html.substring(html.indexOf("END OF ORDER") + 13, html.indexOf("<", html.indexOf("END OF ORDER")+13));
		return orderNumber;
	}
	public String findInvoiceLink(String html) throws Exception
	{
		String link;
		link = html.substring(html.indexOf("EAE: please ignore ampersand sign used for parameter passing") + 74, html.indexOf("\"", html.indexOf("EAE: please ignore ampersand sign used for parameter passing")+74));
		if(!link.contains("/edi-bin")){
			throw new Exception("Link not Found");
		}
		link = link.replace("&amp;", "&");
		return link;
	}



	//test with console
	public static void main(String[] args) {

		Scanner scanner = new Scanner(System.in);
		System.out.println("Enter page up to where you need");
		int page = scanner.nextInt();
		System.out.println("Enter place on the page up to where you need (1- 10)");
		int placeOnPage = scanner.nextInt();

		String[] myArg = new String[3];
		myArg[0] = String.valueOf(page);
		myArg[1] = String.valueOf(placeOnPage);
		HomeBaseTemplateScraper.launch(myArg);


	}
}
