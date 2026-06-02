package xyz.marsavic.gfxlab.entry_points;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import xyz.marsavic.functions.A0;
import xyz.marsavic.functions.A1;
import xyz.marsavic.geometry.Vector;
import xyz.marsavic.gfxlab.ElementAnimationSink;
import xyz.marsavic.gfxlab.playground.GfxLab;
import xyz.marsavic.gfxlab.resources.Resources;
import xyz.marsavic.graph.Graph;
import xyz.marsavic.graph.Vertex;
import xyz.marsavic.graph.VertexInputJack;
import xyz.marsavic.graph.Vertex_AnimationSink_Image;
import xyz.marsavic.javafx.UtilsFX;
import xyz.marsavic.reactions.elements.Element;
import xyz.marsavic.utils.FileWatcher;

import java.io.IOException;
import java.nio.file.Paths;


public class App extends Application {

	static {
		System.setProperty("prism.forceGPU", "true");
//		System.setProperty("javafx.animation.fullspeed", "true");
//		System.setProperty("javafx.animation.pulse", "60");
//		System.setProperty("prism.vsync", "true");
//		System.setProperty("quantum.multithreaded", "false");
	}
	
	Stage primaryStage;
	Pane root;
	Graph graph;
	Region info;
	TextArea textArea;
	
	static final String cssFilePath = "resources/xyz/marsavic/gfxlab/resources/mars-dark2.css";
	
	
	AnimationTimer animationTimerProfiling = new AnimationTimer() {
		long timeLast = Long.MIN_VALUE;
		boolean firstTime = true;
		
		@Override
		public void handle(long timeNow) {
			long timePassed = timeNow - timeLast;
			if (!firstTime && timePassed < 1_000_000_000 / 20) {
				return;
			}
			firstTime = false;
			timeLast = timeNow;
			
			textArea.setText(Profiling.infoTextSystem() + Profiling.infoTextProfilers());
		}
	};
	
	
	
	void addElements(Graph graph, Element e) {
		Vertex vertex = graph.createVertex(e);
		
		for (VertexInputJack inputJack : vertex.inputJacks()) {
			Element.Input<?> input = inputJack.input;
			Element.Output<?> output = input.output();
			Element eChild = output.element();
			addElements(graph, eChild);
			graph.createConnection(output, input);
		}
	}
	
	
	void initGraph(Graph graph) {
		var sink = new ElementAnimationSink(GfxLab.setup());		
		
		addElements(graph, sink);
		
		// Adding onResized for animation sinks 
		for (Vertex v : graph.vertices) {
			if (v instanceof Vertex_AnimationSink_Image vas) {
				vas.onResized().add(onResized);
			}
		}
	}
	
	
	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;
		
		primaryStage.setTitle("GFX Lab");
		
		graph = new Graph();
		initGraph(graph);
		
		textArea = new TextArea();
		textArea.setEditable(false);
		info = textArea;
		
		root = new Pane(
				graph
		);
		
		graph   .prefWidthProperty ().bind(root.widthProperty ());
		graph   .prefHeightProperty().bind(root.heightProperty());
//		textArea.prefWidthProperty ().bind(root.widthProperty ());
//		textArea.prefHeightProperty().bind(root.heightProperty());
		textArea.setPrefWidth(800);
		textArea.setPrefHeight(400);
		
		textArea.setMouseTransparent(true);
		
		Vector sceneSize = UtilsFX.getScreenBox().d().mul(0.9);
		Scene scene = new Scene(root, sceneSize.x(), sceneSize.y());
		
		
		scene.getStylesheets().setAll(Resources.stylesheetURL);
		primaryStage.getIcons().setAll(Resources.iconsApplication());

		primaryStage.setFullScreenExitHint("");
		
		primaryStage.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
			A0 action = switch (event.getCode()) {
				case ESCAPE -> Platform::exit;
				case F1  -> () -> graph.makeLayout(Graph.layout1);
				case F2  -> () -> graph.makeLayout(Graph.layout2);
				case F3  -> () -> graph.makeLayout(Graph.layout3);
				case F8  -> System::gc;
				case F9  -> this::toggleInfo;
				case F11 -> () -> primaryStage.setFullScreen(!primaryStage.isFullScreen());
				default -> A0.NOOP;
			};
			
			action.at();
		});

		primaryStage.setScene(scene);
		primaryStage.show();
		
		Platform.runLater(() -> {
			graph.makeLayout(Graph.layout1);
			graph.centerOnZero();
		});
		
		animationTimerProfiling.start();
		
		try {
			//noinspection resource
			FileWatcher.watchFile(Paths.get(cssFilePath), this::reloadCSS);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void reloadCSS() {
		Platform.runLater(() -> {
			primaryStage.getScene().getStylesheets().setAll("file:" + cssFilePath);
		});
	}
	
	private void toggleInfo() {
		UtilsFX.toggle(root, info);
	}
	
	
	private void makeLayout() {
		graph.makeLayout(Graph.layout1);
	}
	
	
	private final A1<Vertex_AnimationSink_Image.EventResized> onResized = this::onResized;
	
	private void onResized(Vertex_AnimationSink_Image.EventResized eventResized) {
		Platform.runLater(this::makeLayout);
	}
	
	
	public static void main() {
		launch();
	}
	
	
	@Override
	public void stop() {
	}	
	
}
