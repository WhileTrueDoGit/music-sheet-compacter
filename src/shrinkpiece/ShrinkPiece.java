/*
 * This application enables the user to compact a music sheet to a smaller 
 * (marching band) format. It uses image-scaling and blank area filtering. The
 * marchingsheet can be printed (or printed to PDF).
 *
 * Copyright (C) 2020 P.C.T. Kolkman
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package shrinkpiece;

import java.io.*;
import java.util.ArrayList;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.PageRange;
import javafx.print.Paper;
import javafx.print.PrintSides;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import shrinkpiece.GuideRectangle.GuideOrientation;

/**
 *
 * @author P.C.T. Kolkman
 */
public class ShrinkPiece extends Application {
        
    private enum FrameMode {MOVE, ADJ_WIDTH, ADJ_HEIGHT};
    private enum ShrinkPhase {INIT_PASTE, PASTE, INIT_CROP, CROP, SHRINK, INIT_REFACT, REFACT, SAVE};
    private enum Actions {ENTER_TEXT, ESCAPE_TEXT_ENTRY, ESCAPE_SEGNOSEG, REPS_SET_VISIBLE, ESCAPE_REPS,
            ESCAPE_REP_LR, UNGROUP_REP_VIEWS, REP_SIGN_MERGE, IMAGE_IMPORT, PIECE_CROP,
            ESCAPE_SHRINK, SHRINK, DELETE_SHRINK_AREA, ESCAPE_REFACTORING, COMPLETE_REF_SEL, 
            DELETE_SELECTED_AREA, SCALE_REF_AREA, SETTLE_REF_AREA, SAVE_MARCH, SEGNO_MERGE,
            ADD_REF_AREA, SHRUNK_PIECE_PREPEND};
    public Stage stageShrinker;
    public static final double AWIDTH = Screen.getPrimary().getVisualBounds().getWidth(),
            AHEIGHT = Screen.getPrimary().getVisualBounds().getHeight();            
    private final double pDensity = Screen.getPrimary().getDpi()/2.54,
            mWidth = 19.0, mHeight = 14.2, arrWidth = 2.0, dotRad = 3.0,
            DOTS_PER_POINT = 72.0/Screen.getPrimary().getDpi(),
            WIDTH_COP = 525.0, HEIGHT_COP = 600.0;
    private double xOffDSArr, yOffDSArr, sXMPRI, sYMPRI, sXPapRect, sYPapRect, addWPapRectGuide,
            addHPapRectGuide, sXBrush, sYBrush, sXTGon, sYTGon, sXMGon, sYMGon, sXTCI, sYTCI,
            sXLA, sYLA, sXMA, sYMA, xOffTBShadow, yOffTBShadow, xInPPane, yInPPane;
    private int indAreaToRemove, indPrepShrunkPiece;
    private GraphicsContext sPCont;
    private Canvas shrunkPiece;
    private ProgressIndicator progIndicator;
    private Pane splashPane, shrinkPane;
    private ScrollPane sPiecePane;
    private Scene shrinkScene;
    private Group shrinkComponents, importArr, shPieceContent, maskGr, mask, 
                sPieceSPaneContentWShade, clearAreas, resizeArrow, rotateArrow,
                shrunkPieces, textEntry, reps, segnoSeg;
    private GroupTranslatable repsTranslL, repsTranslR;
    private static Group layGuidesInit;
    private GroupButton segnoButton, repsButton;    
    private DropShadow dropShadowArrow, maskShadow, rotateArrowShadow, textButtShadow;
    private Rectangle boundsImportArr, marchBounds, papRectInt;
    private Rectangle paperRectangle, sPiecePaneInteraction;
    private Polygon marchPerifShade, preScaleTransfAGon, transformationAreaGon;
    private Polyline transformationArea;
    private PixelReader pReader, transfSelReader;
    private PixelWriter pWriter, transfSelWriter;
    private Label applicationTitle, copyrightNotice, wLabel, hLabel;
    private Bounds boundsMBounds, boundsPaperRectangle;
    private ColorThreshold papColBox;
    private ShrinkPhase ph, formerPh;
    private boolean byKeyPressHor, byKeyPressVer, shiftPiecePane, ctrPiecePane,
            transfSelDragged, sPiecePaneIntWasVisible, ornamentation;
    private Button hideSplashButton, clearButton, menuButton;
    private ContextMenu shrinkMenu;
    private MenuItem deleteSel, clearAccordingToArea;
    private Node selItem;
    private Canvas brush;
    private WritableImage preModificationImage, sPContImage, cutOutImage;
    private Image piece;
    private ImageView cutImageView, lRepView, rRepView;
    private Line arrShunt, leftPoint, rightPoint;
    private Circle rDot;
    private Color arrColor;
    private String selFontFamily, textEntered;
    private TextField textAddField, fontSize, tFLabel;
    private TextArea copyrightDisplay;
    private Line insertionLine;
    private double[] tPreviewOffset;
    private TwoDimensionalAdjustment repResizing;
    private final Font tooltipFont = Font.font("Cooper Black", 12.0);
    private Text schermafdrukHulp;
    private PrintSelector pPr;    
    private ArrayList<Actions> actionsPerformed;
    private ClearArea removedClearArea;
    private ShrunkPiece shrunkPiecePrepended;
    public final String GPL3_TEXT = "This program is free software: you can redistribute"
                + " it and/or modify\n" +
                " it under the terms of the GNU General Public License as published by\n" +
                " the Free Software Foundation, either version 3 of the License, or\n" +
                " (at your option) any later version.\n" +
                " \n" +
                " This program is distributed in the hope that it will be useful,\n" +
                " but WITHOUT ANY WARRANTY; without even the implied warranty of\n" +
                " MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" +
                " GNU General Public License for more details.\n" +
                " \n" +
                " You should have received a copy of the GNU General Public License\n" +
                " along with this program.  If not, see <http://www.gnu.org/licenses/>.";
    
    @Override
    public void start(Stage primaryStage) {
        /*Init class-variables and show primary stage for further user-
        interaction: */
        stageShrinker = primaryStage;
        splashPane = new Pane();
        shrinkPane = new Pane();
        sPiecePane = new ScrollPane();
        shrinkComponents = new Group();
        importArr = new Group();
        shPieceContent = new Group();
        sPieceSPaneContentWShade = new Group();
        resizeArrow = new Group();
        shrunkPieces = new Group(); 
        textEntry = new Group();
        reps = new Group();
        repsButton = new GroupButton();
        segnoSeg = new Group();        
        repsTranslL = new GroupTranslatable();
        repsTranslR = new GroupTranslatable();
        segnoButton = new GroupButton();        
        shrunkPiece = new Canvas(); 
        papColBox = new ColorThreshold(Color.rgb(250,250,240));
        clearButton = new Button();
        hideSplashButton = new Button();
        mask = new Group();
        progIndicator = new ProgressIndicator();
        schermafdrukHulp = new Text();
        sPiecePaneInteraction = new Rectangle();
        tFLabel = new TextField();
        copyrightNotice = new Label("Copyright (C) 2020 P.C.T. Kolkman");
        applicationTitle = new Label();
        copyrightDisplay = new TextArea();
        transfSelReader = null;
        transfSelWriter = null;
        cutOutImage = null;  
        shrunkPiecePrepended = null;
        removedClearArea = null;
        indAreaToRemove = -1;
        indPrepShrunkPiece = -1;
        preModificationImage = null;
        tPreviewOffset = null;
        pPr = null;
        shiftPiecePane = false;
        ctrPiecePane = false;
        ornamentation = false;
        sPiecePaneIntWasVisible = false;
        actionsPerformed = new ArrayList<Actions>();
        textEntered = "";
        repResizing = new TwoDimensionalAdjustment(); 
        
        shrinkScene = new Scene(splashPane, AWIDTH, AHEIGHT);
        if(!shrinkScene.getStylesheets().add("shrinkpiece/sources/shrinkPieceStyling.css")){
            System.out.println("Style sheet could not be loaded!");
        }
        
        initSplashPane();
        
        ph = ShrinkPhase.INIT_PASTE;
        formerPh = ph;
        initPasteMode();
        
        initResizingArrow();
        initShrunkPiece();        
        initImportArrow();        
        initCopyrightNotice();
        //initMaskBrush();
        initClearButton();
        initTextButton();
        initRepetitionSigns();
        initPapColorBox();
        
        updateTXShrunkPieces();
        shrunkPieces.setTranslateY(sPiecePane.getTranslateY());
        shrunkPieces.getChildren().addListener((ListChangeListener.Change<? extends Node> c) -> {
            orderStroredShrunk();
        });
        
        progIndicator.setTranslateX(AWIDTH*0.5);
        progIndicator.setTranslateY(AHEIGHT*0.5);
        progIndicator.setVisible(false);
                
        shrinkComponents.getChildren().addAll(importArr, copyrightNotice, shrunkPieces, sPiecePane,
                tFLabel, segnoButton, repsButton, segnoSeg,  reps, repResizing, 
                repsTranslL, repsTranslR, papColBox, clearButton, mask, textEntry, 
                progIndicator, schermafdrukHulp); 
        shrinkComponents.setOnMouseMoved((mM)->{
            if(textEntry.isVisible()){
                if(!insertionLine.isVisible()){
                    textEntry.setTranslateX(mM.getX());
                    textEntry.setTranslateY(mM.getY());
                }
            } else if(segnoSeg.isVisible()){
                segnoSeg.setTranslateX(mM.getX());
                segnoSeg.setTranslateY(mM.getY());
            }
        });
        shrinkComponents.setOnMousePressed((mP)->{
            if(textEntry.isVisible()){
                if(!insertionLine.isVisible()){
                    Bounds sPPBounds = sPiecePane.getBoundsInParent();
                    
                    xInPPane = mP.getX() - sPPBounds.getMinX();
                    yInPPane = mP.getY() - sPPBounds.getMinY();
                    if((xInPPane >= 0.0) && (yInPPane >= 0.0) &&
                            (xInPPane < sPPBounds.getMaxX()) && 
                            (yInPPane < (sPPBounds.getMaxY()))){ 
                        textEntry.setTranslateX(mP.getX());
                        textEntry.setTranslateY(mP.getY());
                        insertionLine.setVisible(true);
                        textAddField.requestFocus();
                    } else {
                        hideTextEntry();
                    }
                }
            } else if(segnoSeg.isVisible()){
                Bounds sPPBounds = sPiecePane.getBoundsInParent();                    
                xInPPane = mP.getX() - sPPBounds.getMinX();
                yInPPane = mP.getY() - sPPBounds.getMinY();
                if((xInPPane >= 0.0) && (yInPPane >= 0.0) &&
                        (xInPPane < sPPBounds.getMaxX()) && 
                        (yInPPane < (sPPBounds.getMaxY()))){
                    segnoSeg.setTranslateX(mP.getX());
                    segnoSeg.setTranslateY(mP.getY());
                }
            }
        });               
        
        initStage();        
        stageShrinker.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
    private void initStage(){  
        //Set root-background:
        BackgroundImage bHarp = new BackgroundImage(new Image(getClass().getResourceAsStream("sources/HarpTile.png")),
            BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT, BackgroundPosition.CENTER,BackgroundSize.DEFAULT);
        shrinkPane.setBackground(new Background(bHarp));
        
        //Provide user-info with tooltip:
        Rectangle shrinkPaneTooltipInteraction = new Rectangle(0.0,0.0,
                shrinkPane.getWidth(),shrinkPane.getHeight());
        shrinkPaneTooltipInteraction.setFill(Color.TRANSPARENT);
        shrinkPaneTooltipInteraction.setStroke(Color.TRANSPARENT);
        SPTooltip shrinkPaneTooltip = new SPTooltip("Compact music sheets into marching band format.\n" +
                                                    "Import clipboard-images (from 'PrtSc' key) with the arrow\n" +
                                                    "or 'ENTER'. To proceed, press 'ENTER'."
        shrinkPaneTooltip.setHideOnEscape(true);
        Tooltip.install(shrinkPaneTooltipInteraction, shrinkPaneTooltip);
        
        shrinkPane.getChildren().addAll(shrinkPaneTooltipInteraction, shrinkComponents);        
        
        sPiecePane.requestFocus();
        
        //When resizing stageShrinker window (in future), move components along:
        stageShrinker.setMinHeight(200.0);
        stageShrinker.setMinWidth(100.0);
        stageShrinker.heightProperty().addListener((ObservableValue<? extends Number> obs, 
                Number oHeight, Number nHeight) -> {
                    moveCopyrightViewer();
                    moveCopyrightNotice();                    
                });
        stageShrinker.widthProperty().addListener((ObservableValue<? extends Number> o,
                Number oWidth, Number nWidth) -> {
                    moveCopyrightViewer();
                    moveCopyrightNotice();
                });
        
        //When closing stageShrinker window, also close any open printing window:
        stageShrinker.setOnCloseRequest((wC) ->
             { if(pPr != null && pPr.isShowing()) pPr.close(); }
        );
        stageShrinker.setMaximized(true);
        stageShrinker.setTitle("Shrink Piece");        
        stageShrinker.setScene(shrinkScene);
    }
    
    private void initPasteMode(){
        sXPapRect = 0.0; sYPapRect = 0.0;
        sXMPRI = 0.0;  sYMPRI = 0.0; 
        sXTCI = 0.0; sYTCI = 0.0;
        sXLA = 0.0; sYLA = 0.0;
        sXMA = 0.0; sYMA = 0.0;
        addWPapRectGuide = 0.0;
        addHPapRectGuide = 0.0;
        pReader = null; pWriter = null;
        selItem = null;
        sPContImage = null;
        //mask.setVisible(false);
        boundsPaperRectangle = null;
        byKeyPressHor = false; 
        byKeyPressVer = false;
        transfSelDragged = false; 
        sPiecePaneInteraction.setVisible(false);
        cutImageView = new ImageView();
        setInitWidthSPiecePane();
    }
    
    private void initShrunkPiece(){ 
        //Presentation of loaded and manipulated sheet of music:        
        layGuidesInit = new Group();
        clearAreas = new Group();
        rotateArrow = new Group();
        marchBounds = new Rectangle(0.0, 0.0, pDensity*mWidth, pDensity*mHeight);
        paperRectangle = new Rectangle(0.0, 0.0,
            shrunkPiece.getWidth(), shrunkPiece.getHeight());
        papRectInt = new Rectangle(); 
        marchPerifShade = new Polygon();
        transformationArea = new Polyline();
        transformationAreaGon = new Polygon();
        preScaleTransfAGon = new Polygon();
        menuButton = new Button();
        
        sPieceSPaneContentWShade.getChildren().addAll(shPieceContent, sPiecePaneInteraction,
                cutImageView, transformationArea, transformationAreaGon, resizeArrow, 
                marchPerifShade, menuButton);       
        sPiecePane.setContent(sPieceSPaneContentWShade);        
        initSPiecePane();

        initShrinkContextMenu();
        
        shrunkPiece.setTranslateX(0.0);
        shrunkPiece.setTranslateY(0.0);
        shrunkPiece.setWidth(pDensity*mWidth);
        shrunkPiece.setHeight(pDensity*mHeight);
        sPCont = shrunkPiece.getGraphicsContext2D();
        
        marchBounds.setFill(Color.TRANSPARENT);
        marchBounds.setStrokeType(StrokeType.OUTSIDE);
        marchBounds.setStroke(Color.BLACK);
        marchBounds.setStrokeWidth(2.0);
        marchBounds.setMouseTransparent(true);
        marchBounds.boundsInParentProperty().addListener((ObservableValue<? extends Bounds> bsMB,
                Bounds oBsMB, Bounds nBsMB) -> {
            boundsMBounds = nBsMB;
            updateMarchBoundsLabels();
            switch(ph){
                case INIT_PASTE:
                case PASTE:
                case CROP:
                    //Skip;
                    break;
                default:
                    adjustWidthPiecePaneToMBounds();
            }
        });
        boundsMBounds = marchBounds.getBoundsInParent();
        
        wLabel = new Label();
        hLabel = new Label();
        updateMarchBoundsLabels();         
        
        paperRectangle.setFill(Color.TRANSPARENT);
        paperRectangle.setStroke(Color.CYAN);
        paperRectangle.setStrokeWidth(1.0);
        paperRectangle.setMouseTransparent(true);
        paperRectangle.setStrokeType(StrokeType.OUTSIDE);
        paperRectangle.translateXProperty().addListener((ObservableValue<? extends Number> tXPR,
                Number tXPRFormer, Number newTXPR)->{
            papRectInt.setTranslateX(((double)newTXPR) - addWPapRectGuide); 
        });
        paperRectangle.translateYProperty().addListener((ObservableValue<? extends Number> tYPR,
                Number tYPRFormer, Number newTYPR)->{
            papRectInt.setTranslateY(((double)newTYPR) - addHPapRectGuide);
        });
        paperRectangle.boundsInParentProperty().addListener((ObservableValue<? extends Bounds> observable,
                Bounds oBsPR, Bounds nBsPR) -> {
            double dW, dH;
            dW = oBsPR.getWidth() - nBsPR.getWidth();
            dH = oBsPR.getHeight() - nBsPR.getHeight();
            if (dW != 0.0){
                papRectInt.setWidth(nBsPR.getWidth()- (paperRectangle.getStrokeWidth()*2.0) + (addWPapRectGuide*2.0));
            } else if (dH != 0.0){
                papRectInt.setHeight(nBsPR.getHeight()- (paperRectangle.getStrokeWidth()*2.0) + (addHPapRectGuide*2.0));            
            }
            updatePaperFrameGuides();
        });
        layGuidesInit.setVisible(false);
        layGuidesInit.getChildren().addAll(new GuideRectangle(paperRectangle, GuideOrientation.TOP),
                new GuideRectangle(paperRectangle, GuideOrientation.RIGHT),
                new GuideRectangle(paperRectangle, GuideOrientation.BOTTOM),
                new GuideRectangle(paperRectangle, GuideOrientation.LEFT)
        ); 
        layGuidesInit.visibleProperty().addListener((ObservableValue<? extends Boolean> vis,
                Boolean lGWasVisible, Boolean lGIsVisible)->{
            if (lGIsVisible){
                for(Node gR : layGuidesInit.getChildren()){
                    ((GuideRectangle) gR).takePosition();
                }
            }
        });        
        papRectInt.setFill(Color.TRANSPARENT);
        papRectInt.setStroke(Color.TRANSPARENT);
        addWPapRectGuide = (((GuideRectangle)layGuidesInit.getChildren().get(1)).getWidthGuide()*0.5) + 2.0;
        addHPapRectGuide = (((GuideRectangle)layGuidesInit.getChildren().get(0)).getHeightGuide()*0.5) + 2.0;  
        papRectInt.setWidth(paperRectangle.getBoundsInLocal().getWidth() + (2.0*addWPapRectGuide));
        papRectInt.setHeight(paperRectangle.getBoundsInLocal().getHeight() + (2.0*addHPapRectGuide));   
        papRectInt.setTranslateX(paperRectangle.getTranslateX() - addWPapRectGuide);
        papRectInt.setTranslateY(paperRectangle.getTranslateY() - addHPapRectGuide);
        papRectInt.setOnMouseEntered((mE)->{
            mouseEnterPaperFrame();            
        });
        papRectInt.setOnMouseExited((mE)->{
            Bounds pRIBounds = papRectInt.getBoundsInLocal();            
            if(!((mE.getX() >= pRIBounds.getMinX()) && (mE.getX() <= pRIBounds.getMaxX()) &&
                    (mE.getY() >= pRIBounds.getMinY()) && (mE.getY() <= pRIBounds.getMaxY()))){
                mouseExitPaperFrame();
            }            
        });
        papRectInt.setOnMousePressed((mP)->{
            sXPapRect = paperRectangle.getTranslateX();
            sYPapRect = paperRectangle.getTranslateY();
            sXMPRI = mP.getScreenX();  sYMPRI = mP.getScreenY();
        });
        papRectInt.setOnMouseDragged((mD)->{
            double dPix;
            dPix = sXMPRI - mD.getScreenX();
            paperRectangle.setTranslateX(sXPapRect - dPix);
            dPix = sYMPRI - mD.getScreenY();
            paperRectangle.setTranslateY(sYPapRect - dPix);
        });
        
        sPiecePaneInteraction.setFill(Color.TRANSPARENT);
        sPiecePaneInteraction.setVisible(false);
        
        marchPerifShade.setFill(Color.WHITE);
        marchPerifShade.setStroke(Color.TRANSPARENT);
        marchPerifShade.setOpacity(0.5);
        marchPerifShade.setMouseTransparent(true);
        marchPerifShade.setVisible(false);  
        shPieceContent.boundsInParentProperty().addListener((ObservableValue<? extends Bounds> bsSPP,
                Bounds oBsSPP, Bounds nBsSPP) -> {
            fadeMarchPerifery(nBsSPP);
            updateSPPaneInteractionSize(nBsSPP);
        }); 
        
        transformationArea.setVisible(false);
        transformationArea.setStroke(Color.SLATEGRAY);
        transformationArea.setStrokeWidth(3.0);
        transformationArea.setStrokeType(StrokeType.INSIDE);
        transformationArea.setOnMousePressed((mP)->{
            if(mP.getButton().equals(MouseButton.SECONDARY)){
                showShrinkContextMenu(transformationAreaGon, mP.getScreenX(), mP.getScreenY());
            }
        });
        transformationAreaGon.setVisible(false);
        transformationAreaGon.setFill(Color.WHITE);
        transformationAreaGon.setOpacity(0.2);
        transformationAreaGon.setStroke(transformationArea.getStroke());
        transformationAreaGon.setStrokeWidth(transformationArea.getStrokeWidth());
        transformationAreaGon.setStrokeType(StrokeType.INSIDE);
        transformationAreaGon.setOnMousePressed((mP)->{
            if(mP.getButton().equals(MouseButton.SECONDARY)){
                showShrinkContextMenu(transformationAreaGon, mP.getScreenX(), mP.getScreenY());
            } else {
                sXTGon = transformationAreaGon.getTranslateX();
                sYTGon = transformationAreaGon.getTranslateY();
                sXMGon = mP.getSceneX();
                sYMGon = mP.getSceneY();  
                transferImage();  
                if(cutImageView.isVisible()){
                    sXTCI = cutImageView.getTranslateX();
                    sYTCI = cutImageView.getTranslateY();
                }
            }
        });
        transformationAreaGon.setOnMouseDragged((mD)->{
            transformationAreaGon.setTranslateX(sXTGon + (mD.getSceneX() - sXMGon));
            transformationAreaGon.setTranslateY(sYTGon + (mD.getSceneY() - sYMGon));
            if(cutImageView.getImage() != null){
                cutImageView.setTranslateX(sXTCI + (mD.getSceneX() - sXMGon));
                cutImageView.setTranslateY(sYTCI + (mD.getSceneY() - sYMGon));
            }
            resizeArrow.setVisible(false);
            transfSelDragged = true;
        });
        transformationAreaGon.setOnMouseReleased((mR)->{
            if(transfSelDragged){
                transfSelDragged = false;
                settlePieceExcerpt();
                resetTransformationSelection();
                actionsPerformed.add(Actions.SETTLE_REF_AREA);
            }
        });   
        
        initRotateArrow();
        
        shPieceContent.getChildren().addAll(shrunkPiece, paperRectangle, papRectInt, 
                layGuidesInit, clearAreas, marchBounds, hLabel, wLabel, rotateArrow);          
    }
    
    private void updateMarchBoundsLabels(){
        /*The marching sheet can be rotated horizontally or vertically, the borders
        respond accordingly:*/
        wLabel.setText(String.format("%.1f", (boundsMBounds.getWidth() -
                (marchBounds.getStrokeWidth()*2.0))/pDensity) + " cm");
        wLabel.setTranslateX(boundsMBounds.getMinX() + (boundsMBounds.getWidth()*0.5) - 30.0);
        wLabel.setTranslateY(boundsMBounds.getMinY() - 17.0);
        hLabel.setText(String.format("%.1f", (boundsMBounds.getHeight()-
                (marchBounds.getStrokeWidth()*2.0))/pDensity) + "\n cm");
        hLabel.setTranslateX(boundsMBounds.getMinX() - 27.0);
        hLabel.setTranslateY(boundsMBounds.getMinY() + (boundsMBounds.getHeight()*0.5) - 10.0);
    }
    
    private void initRotateArrow(){
        //Clickable arrow to re-orient the marching sheet: 
        double[] xDARotateArrowCanv = new double[]{6.0, 6.014506059746964, 6.058019510318786, 6.130526167249968, 6.232002394902452, 6.362415114170119, 6.5217218132619905, 6.709870561560194, 6.926800026548278, 7.172439493804177, 7.446708890051752, 7.749518809262952, 8.08077054180228, 8.440356106604042, 8.828158286371831, 9.244050665789132, 9.687897672727672, 10.159554622441476, 10.65886776473053, 11.185674334060764, 11.739802602621467, 12.321071936305543, 12.929292853591903, 13.564267087312885, 14.225787649284769, 14.91363889778188, 15.627596607830924, 16.367428044303892, 17.132892037784586, 17.923739063184883, 18.739711321084485, 19.580542821768177, 20.445959471933065, 21.335679164036605, 22.249411868258505, 23.18685972704361, 24.147717152197572, 25.131670924502032, 26.138400295817405, 27.167577093640205, 28.218865828080027, 29.291923801222424, 30.386401218841172, 31.50194130442361, 32.638180415472334, 33.79474816204515, 34.97126752749409, 36.16735499136496, 37.38262065441711, 38.616668365722035, 39.869095851800466, 41.13949484775469, 42.427451230354734, 43.73254515303341, 45.05435118274772, 46.39243843866069, 47.746370732599814, 49.1157067112448, 50.499999999999716, 51.8987993485016, 53.31164877771835, 54.73808772858871, 56.17765121215416, 57.62986996113614, 59.09427058290669, 60.57037571380499, 62.05770417474724, 63.55577112808152, 65.06408823563402, 66.58216381789742, 68.10950301430762, 69.64560794455832, 71.18997787089893, 72.74210936136501, 74.30149645388593, 75.86763082121843, 77.44000193664994, 79.01809724041999, 80.6014023068035, 82.18940101180294, 83.78157570139376, 85.37740736026876, 86.9763757810257, 88.5779597337447, 90.1816371358974, 91.78688522253515, 93.39318071669959, 94.99999999999955};
        double[] yDARotateArrowCanv = new double[]{91.0, 89.46539731370234, 87.93129487545542, 86.39819277023952, 84.86659075694763, 83.33698810547446, 81.80988343396496, 80.28577454627543, 78.7651582696999, 77.24853029301505, 75.73638500489596, 74.22921533275604, 72.72751258206301, 71.23176627618369, 69.7424639968097, 68.26009122501631, 66.7851311830056, 65.3180646765864, 63.85936993844177, 62.40952247223521, 60.96899489760642, 59.53825679610736, 58.11777455812876, 56.70801123086642, 55.309426367377625, 53.9224758767769, 52.54761187561883, 51.18528254051796, 49.835931962052484, 48.500000000000114, 47.177922139953296, 45.870129351359765, 44.57704794703591, 43.29909944419762, 42.03670042705477, 40.790262411013316, 39.56019170853011, 38.346889296663676, 37.15075068636412, 35.97216579354483, 34.81151881197815, 33.669188088056444, 32.54554599745916, 31.44095882376604, 30.35578663905659, 29.29038318653403, 28.245095765212795, 27.220265116705946, 26.216225314151075, 25.23330365330912, 24.271820545873027, 23.332089415020448, 22.414416593244198, 21.519101222494328, 20.646435156664438, 19.796702866453188, 18.970181346632785, 18.16714002575509, 17.387840678322846, 16.63253733945743, 15.901476222088206, 15.19489563669265, 14.513025913612069, 13.856089327968789, 13.224300027209097, 12.617863961295825, 12.036978815572866, 11.481833946324059, 10.952610319046926, 10.44948044946193, 9.972608347275695, 9.522149462717607, 9.09825063586618, 8.701050048782236, 8.33067718046459, 7.987252764642733, 7.670888750419749, 7.3816882657798715, 7.119745583970371, 6.885146092770924, 6.6779662666587, 6.49827364187945, 6.346126794432166, 6.221575320974296, 6.1246598226545075, 6.0554118918774975, 6.013854102005496, 6.0};
        double[] xDARotateArrowCanvB = new double[]{6.0, 17.498291891923316, 17.498291891923316, 27.87035549064217};
        double[] yDARotateArrowCanvB = new double[]{6.0, 14.353998058901539, 14.353998058901539, 6.8182527512559545};
        double[] xDARotateArrowCanvC = new double[]{6.0, 17.498291891923373, 17.498291891923373, 9.844394852014602};
        double[] yDARotateArrowCanvC = new double[]{6.0, 14.353998058901539, 14.353998058901539, 24.888683565779843};

        //Transparent and mouse transparent rectangle surrounding group at base:
        Rectangle backgroundRotateArrow = new Rectangle(0.0,0.0,117.498291015625, 107.35400390625);
        backgroundRotateArrow.setFill(Color.TRANSPARENT);
        backgroundRotateArrow.setStroke(Color.TRANSPARENT);
        backgroundRotateArrow.setMouseTransparent(true);
        rotateArrow.getChildren().add(backgroundRotateArrow);

        Canvas rotateArrowCanv = new Canvas(100.99999999999955, 97.0);
        GraphicsContext grConRotateArrowCanv = rotateArrowCanv.getGraphicsContext2D();
        grConRotateArrowCanv.setStroke(Color.valueOf("0x000000ff"));
        grConRotateArrowCanv.setLineWidth(4.0);
        grConRotateArrowCanv.setLineJoin(StrokeLineJoin.ROUND);
        grConRotateArrowCanv.strokePolyline(xDARotateArrowCanv, yDARotateArrowCanv, 88);
        rotateArrowCanv.setTranslateX(-36.0);
        rotateArrowCanv.setTranslateY(-26.0);
        Canvas rotateArrowCanvB = new Canvas(33.87035549064217, 20.35399805890154);
        GraphicsContext grConRotateArrowCanvB = rotateArrowCanvB.getGraphicsContext2D();
        grConRotateArrowCanvB.setStroke(Color.valueOf("0x000000ff"));
        grConRotateArrowCanvB.setLineWidth(4.0);
        grConRotateArrowCanvB.setLineJoin(StrokeLineJoin.ROUND);
        grConRotateArrowCanvB.strokePolyline(xDARotateArrowCanvB, yDARotateArrowCanvB, 4);
        rotateArrowCanvB.setTranslateX(-47.5);
        rotateArrowCanvB.setTranslateY(52.5);
        Canvas rotateArrowCanvC = new Canvas(23.498291891923373, 30.888683565779843);
        GraphicsContext grConRotateArrowCanvC = rotateArrowCanvC.getGraphicsContext2D();
        grConRotateArrowCanvC.setStroke(Color.valueOf("0x000000ff"));
        grConRotateArrowCanvC.setLineWidth(4.0);
        grConRotateArrowCanvC.setLineJoin(StrokeLineJoin.ROUND);
        grConRotateArrowCanvC.strokePolyline(xDARotateArrowCanvC, yDARotateArrowCanvC, 4);
        rotateArrowCanvC.setTranslateX(46.5);
        rotateArrowCanvC.setTranslateY(-34.5);
        
        rotateArrowShadow = new DropShadow();
        rotateArrowShadow.setOffsetX(4.0);
        rotateArrowShadow.setOffsetY(3.0);

        rotateArrow.getChildren().addAll(rotateArrowCanv, rotateArrowCanvB, rotateArrowCanvC);
        rotateArrow.setScaleX(0.40);
        rotateArrow.setScaleY(0.40);
        rotateArrow.setTranslateX(-24.0);
        rotateArrow.setTranslateY(-24.0);
        rotateArrow.setEffect(rotateArrowShadow);        
        rotateArrow.setOnMouseEntered((mE)->{
            rotateArrowShadow.setOffsetX(6.0);
            rotateArrowShadow.setOffsetY(5.0);
        });
        rotateArrow.setOnMouseExited((mEx)->{            
            rotateArrowShadow.setOffsetX(4.0);
            rotateArrowShadow.setOffsetY(3.0);
        });
        rotateArrow.setOnMousePressed((mP)->{
            double wMarch;
            rotateArrow.setTranslateX(rotateArrow.getTranslateX() + 4.0);
            rotateArrow.setTranslateY(rotateArrow.getTranslateY() + 3.0);
            rotateArrow.setEffect(null);
            
            wMarch = marchBounds.getWidth();
            marchBounds.setWidth(marchBounds.getHeight());
            marchBounds.setHeight(wMarch);
        });
        rotateArrow.setOnMouseReleased((mP)->{
            rotateArrow.setEffect(rotateArrowShadow);
            rotateArrow.setTranslateX(rotateArrow.getTranslateX() - 4.0);
            rotateArrow.setTranslateY(rotateArrow.getTranslateY() - 3.0);
        });
    }
    
    private void initShrinkContextMenu(){
        menuButton.setVisible(false);
        shrinkMenu = new ContextMenu();
        deleteSel = new MenuItem("Delete marking");
        deleteSel.setOnAction((ac)->{
            String sName;
            
            sName = selItem.getClass().getSimpleName();
            switch(sName){
                case "ClearArea":
                    deleteClearingArea((ClearArea) selItem);
                    actionsPerformed.add(Actions.DELETE_SHRINK_AREA);
                    break;
                case "Polyline":
                case "Polygon":
                    resetTransformationSelection();
                    break;
            }            
        });        
        clearAccordingToArea = new MenuItem("Delete the marked area");
        clearAccordingToArea.setOnAction((ac)->{
            deleteMarkedArea();
        });
        shrinkMenu.getItems().addAll(deleteSel, clearAccordingToArea);
        menuButton.setContextMenu(shrinkMenu);
    }
    
    private void setInitWidthSPiecePane(){        
        sPiecePane.setTranslateX(70.0);
        sPiecePane.setPrefWidth((AWIDTH*0.95) - 70.0);      
    }
    
    private void adjustWidthPiecePaneToMBounds(){        
        sPiecePane.setPrefWidth(boundsMBounds.getWidth() + 50.0);
        sPiecePane.setTranslateX((AWIDTH*0.5) - (sPiecePane.getPrefWidth()*0.5));
    }           

    private void initSPiecePane(){  
        sPiecePane.getStyleClass().add("colThr");
        sPiecePane.boundsInParentProperty().addListener(new ChangeListener<Bounds>(){
            @Override
            public void changed(ObservableValue<? extends Bounds> observable, 
                    Bounds oBSPPane, Bounds nBSPPane) {
                if(nBSPPane.getMinX() != oBSPPane.getMinX()){                    
                    updateTXShrunkPieces();  
                }
            }
        });
        setInitWidthSPiecePane();
        sPiecePane.setTranslateY(150.0);
        sPiecePane.setPrefHeight((AHEIGHT-sPiecePane.getTranslateY())*0.9);
        sPiecePane.setFitToHeight(false);
        sPiecePane.setFitToWidth(false);
        sPiecePane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sPiecePane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sPiecePane.setPannable(false);
        sPiecePane.hvalueProperty().addListener((ObservableValue<? extends Number> hVSPP,
                Number oldHVSPP, Number newHVSPP)->{
            if (byKeyPressHor){
                byKeyPressHor = false;
                sPiecePane.setHvalue((double) oldHVSPP);
            }
        });
        sPiecePane.vvalueProperty().addListener((ObservableValue<? extends Number> hVSPP,
                Number oldHVSPP, Number newHVSPP)->{
            if (byKeyPressVer){
                byKeyPressVer = false;
                sPiecePane.setVvalue((double) oldHVSPP);
            }
        }); 
        sPiecePane.setOnKeyPressed((kP)->{  
            double formerStretch;            
            
            shiftPiecePane = kP.isShiftDown();
            ctrPiecePane = kP.isControlDown();
            
            if(kP.getCode().equals(KeyCode.Z)){
                if(kP.isControlDown()){
                    handleUndoRequest();
                    ornamentation = false;
                }
            } else if (ornamentation){
                switch(kP.getCode()){
                    case ESCAPE:
                    case BACK_SPACE:
                        if(textEntry.isVisible()){
                            hideTextEntry();
                            actionsPerformed.add(Actions.ESCAPE_TEXT_ENTRY);
                        } else if(segnoSeg.isVisible()) {
                            segnoSeg.setVisible(false);
                            actionsPerformed.add(Actions.ESCAPE_SEGNOSEG);
                        } else if(reps.isVisible()){
                            escapeRepPlacement();
                            actionsPerformed.add(Actions.ESCAPE_REPS);
                        } else {
                            if(repsTranslL.isVisible()){
                                repsTranslL.setVisible(false);
                            }
                            if (repsTranslR.isVisible()){
                                repsTranslR.setVisible(false);
                            }
                            actionsPerformed.add(Actions.ESCAPE_REP_LR);
                        }
                        ornamentation = false;
                        break;
                    case SPACE:
                    case ENTER:
                        if(repResizing.isVisible()){
                            repResizing.setVisible(false);
                            ungroupRepViews();
                            actionsPerformed.add(Actions.UNGROUP_REP_VIEWS);
                        } else if(repsTranslL.isVisible() ||
                                repsTranslR.isVisible()){
                            mergeSignWithScore();
                            ornamentation = false;
                            actionsPerformed.add(Actions.REP_SIGN_MERGE);
                        }                        
                        break;                    
                }
            } else {
                switch(ph){
                    case INIT_PASTE:
                        switch(kP.getCode()){
                            case V:
                                if (!kP.isControlDown()){
                                    break;
                                }
                            case ENTER:
                                importClipboardImage();
                                actionsPerformed.add(Actions.IMAGE_IMPORT);
                                break;
                        }        
                        break;
                    case INIT_CROP:                
                        switch(kP.getCode()){                        
                            case BACK_SPACE:
                            case DELETE:
                            case ENTER:
                                cropImage();
                                setClearAreas();
                                actionsPerformed.add(Actions.PIECE_CROP);
                                break;
                            case LEFT:                            
                                if (paperRectangle.isVisible()){
                                    byKeyPressHor = true;
                                    if(shiftPiecePane){
                                        formerStretch = paperRectangle.getBoundsInLocal().getWidth() -
                                                (paperRectangle.getStrokeWidth()*2.0);
                                        if (formerStretch >= 4.0){
                                            paperRectangle.setWidth(formerStretch -
                                                    4.0);   
                                        }                                       
                                    } else {                                    
                                        if (paperRectangle.getTranslateX() >= 4.0){
                                            paperRectangle.setTranslateX(
                                                paperRectangle.getTranslateX() - 4.0);
                                        }
                                    }
                                }                            
                                break;
                            case RIGHT: 
                                if (paperRectangle.isVisible() && (shrunkPiece != null)){                                
                                    byKeyPressHor = true;
                                    if(shiftPiecePane){  
                                        formerStretch = paperRectangle.getBoundsInLocal().getWidth() -
                                                (paperRectangle.getStrokeWidth()*2.0);
                                        paperRectangle.setWidth(formerStretch +
                                                5.0); 
                                    } else {
                                        if (paperRectangle.getTranslateX() <= 
                                                (shrunkPiece.getWidth() - 5.0)){
                                            paperRectangle.setTranslateX(
                                                paperRectangle.getTranslateX() + 5.0);
                                        } else {
                                            paperRectangle.setTranslateX(shrunkPiece.getWidth());
                                        }
                                    }
                                }
                                break;
                            case UP:                                 
                                if (paperRectangle.isVisible()){
                                    byKeyPressVer = true;
                                    if(shiftPiecePane){
                                        formerStretch = paperRectangle.getBoundsInLocal().getHeight() -
                                                (paperRectangle.getStrokeWidth()*2.0);
                                        if (formerStretch >= 4.0){
                                            paperRectangle.setHeight(formerStretch -
                                                    4.0);   
                                        }
                                    } else { 
                                        if (paperRectangle.getTranslateY() >= 
                                                4.0){
                                            paperRectangle.setTranslateY(
                                                paperRectangle.getTranslateY() - 4.0);
                                        }
                                    }
                                }
                                break;
                            case DOWN:
                                if (paperRectangle.isVisible() && (shrunkPiece != null)){
                                    byKeyPressVer = true;
                                    if(shiftPiecePane){
                                        formerStretch = paperRectangle.getBoundsInLocal().getHeight() -
                                                (paperRectangle.getStrokeWidth()*2.0);
                                        paperRectangle.setHeight(formerStretch + 5.0);                                  
                                    } else {                                
                                        if (paperRectangle.getTranslateY() <= 
                                                (shrunkPiece.getHeight() + 5.0)){
                                            paperRectangle.setTranslateY(
                                                paperRectangle.getTranslateY() + 5.0);
                                        } else {
                                            paperRectangle.setTranslateY(shrunkPiece.getHeight());                                        
                                        }
                                    }                                
                                }
                                break;                    
                        }                    
                        break;
                    case SHRINK:
                        switch(kP.getCode()){
                            case ESCAPE:
                                escapeClearing();
                                actionsPerformed.add(Actions.ESCAPE_SHRINK);
                                break;
                            case ENTER:
                            case DELETE:
                                clearButtonAction();
                                actionsPerformed.add(Actions.SHRINK);
                                break;
                        }
                        break;
                    case INIT_REFACT:
                    case REFACT:
                        switch(kP.getCode()){
                            case ESCAPE:
                                resetTransformationSelection();
                                actionsPerformed.add(Actions.ESCAPE_REFACTORING);
                                break;
                            case ENTER:
                            case SPACE:
                                if(transformationArea.isVisible()){                                
                                    completeSelectionTransformation();  
                                    actionsPerformed.add(Actions.COMPLETE_REF_SEL);
                                } else if (transformationAreaGon.isVisible()) {
                                    deleteMarkedArea();
                                    actionsPerformed.add(Actions.DELETE_SELECTED_AREA);
                                } else {
                                    saveMarch();
                                    actionsPerformed.add(Actions.SAVE_MARCH);
                                }
                                break;
                            case DELETE:
                            case BACK_SPACE:
                                deleteMarkedArea();
                                actionsPerformed.add(Actions.DELETE_SELECTED_AREA);
                                break;
                        }
                }
            }
        });            
        sPiecePane.setOnKeyReleased((kR)->{
            switch(kR.getCode()){
                case SHIFT:
                    shiftPiecePane = false;
                    break;
                case CONTROL:
                    ctrPiecePane = false;
                    break;
            }
        });
        sPiecePane.setOnMousePressed((mP)->{
            if(segnoSeg.isVisible()){
                mergeSignWithScore();                
                ornamentation = false;
            }
        });
        sPiecePaneInteraction.setOnMousePressed((mP)->{
            if((!ornamentation) && (ph.equals(ShrinkPhase.INIT_REFACT) ||
                    ph.equals(ShrinkPhase.REFACT))){
                double xP, yP;
                
                formerPh = ph;
                ph = ShrinkPhase.REFACT;
                
                xP = mP.getX();
                yP = mP.getY();
                if(xP < 0.0){
                    xP = 0.0;
                }
                if (yP < 0.0){
                    yP = 0.0;
                }
                transformationArea.getPoints().clear();
                transformationArea.getPoints().add(xP);
                transformationArea.getPoints().add(yP);
                transformationArea.setVisible(true);   
                actionsPerformed.add(Actions.ADD_REF_AREA);
            }
        });
        sPiecePaneInteraction.setOnMouseDragged((mD)->{
            if((!ornamentation) && (ph.equals(ShrinkPhase.REFACT))){
                double xP, yP, maxX, maxY;
                
                xP = mD.getX();
                yP = mD.getY();
                
                if(sPContImage != null){
                    maxX = sPContImage.getWidth();
                    maxY = sPContImage.getHeight();
                } else if(shrunkPiece != null){
                    maxX = shrunkPiece.getWidth();
                    maxY = shrunkPiece.getHeight();
                } else {
                    maxX = sPiecePane.getPrefWidth();
                    maxY = sPiecePane.getPrefHeight();
                }
                maxX -= 1.0;
                maxY -= 1.0;
                
                if(xP < 0.0){
                    xP = 0.0;
                } else if(xP > maxX){
                    xP = maxX;                    
                }
                if(shiftPiecePane || ctrPiecePane){
                    if(transformationArea.getPoints().size() > 1){
                        yP = transformationArea.getPoints().get(
                                transformationArea.getPoints().size() - 1);
                    }
                }
                if (yP < 0.0){
                    yP = 0.0;
                } else if(yP > maxY){
                    yP = maxY;                    
                }
                transformationArea.getPoints().add(xP);
                transformationArea.getPoints().add(yP);                      
                actionsPerformed.add(Actions.ADD_REF_AREA);
            }
        });
        sPiecePaneInteraction.setOnMouseReleased((mR)->{
            if((!ornamentation) && (ph.equals(ShrinkPhase.REFACT))){
                if(transformationArea.getPoints().size() > 0){
                    completeSelectionTransformation();                       
                    actionsPerformed.add(Actions.COMPLETE_REF_SEL);
                }
            }
        });
    }
    
    private void clearShrunkPieceContext(){
        if ((shrunkPiece != null) && (sPCont != null)){
            sPCont.clearRect(0.0,0.0, 
                    shrunkPiece.getWidth(), shrunkPiece.getHeight());
        }
    }
    
    private void importClipboardImage(){
        
        progIndicator.setVisible(true);
        
        generateSPGraphicsRecovery();        
        storeSPContImage();     
        
        formerPh = ph;
        ph = ShrinkPhase.PASTE;
        initPasteMode();        
        
        Clipboard shPCb = Clipboard.getSystemClipboard();
        if (shPCb.hasContent(DataFormat.IMAGE)){
            schermafdrukHulp.setVisible(false);
            piece = shPCb.getImage();
            shrunkPiece.setWidth(piece.getWidth());
            shrunkPiece.setHeight(piece.getHeight());
            clearShrunkPieceContext();
            sPCont.drawImage(piece, 0.0, 0.0);            
            pWriter = sPCont.getPixelWriter();    
            storeShrunkPieceGraphics();
            marchPerifShade.setVisible(true);
            clearAreaSurroundingSheet();
        } else {
            schermafdrukHulp.setVisible(true);
        }
        
        progIndicator.setVisible(false);
    }
    
    private void undoImportClipboardImage(){
        if(shrunkPieces.getChildren().size() > 0){
            shrunkPieces.getChildren().remove(shrunkPieces.getChildren().size() - 1);
            ph = formerPh;            
            recoverShrunkPieceGraphics();
        } else {
            ph = ShrinkPhase.INIT_PASTE;            
            recoverShrunkPieceGraphics();
            sPContImage = null;
        }
        sPiecePaneInteraction.setVisible(sPiecePaneIntWasVisible);
        paperRectangle.setVisible(false);
        mouseExitPaperFrame();
        papRectInt.setVisible(false); 
    }
    
    private void moveCopyrightViewer(){
        double titleBottom = 0.0, dispBottom = 0.0, vertSpacer = 0.0,
               critHeight = HEIGHT_COP + 140.0,
               availableHeight = stageShrinker.getHeight() - critHeight,
               titleStart;
        if(applicationTitle != null){
            titleStart = availableHeight*0.5;
            if(titleStart < 0.0) titleStart = 0.0;
            applicationTitle.setTranslateY(titleStart);
            titleBottom = titleStart + 50.0;
            applicationTitle.setTranslateX(stageShrinker.getWidth()*0.5 - 85.0);
        }
        if(copyrightDisplay != null){
            if(stageShrinker.getWidth() < WIDTH_COP){
                copyrightDisplay.setTranslateX(5.0);
                copyrightDisplay.setPrefWidth(stageShrinker.getWidth() - 10.0);
            } else {
                copyrightDisplay.setTranslateX((stageShrinker.getWidth()-WIDTH_COP)*0.5);
                copyrightDisplay.setPrefWidth(WIDTH_COP);
            }       
            vertSpacer = stageShrinker.getHeight()*0.25;
            copyrightDisplay.setTranslateY(titleBottom);
            if(availableHeight < 0.0){ 
                copyrightDisplay.setPrefHeight(HEIGHT_COP + availableHeight);
            } else {          
                copyrightDisplay.setPrefHeight(HEIGHT_COP);
            }
            dispBottom = copyrightDisplay.getTranslateY() + 
                    copyrightDisplay.getPrefHeight();
            
        }
        if(hideSplashButton != null){
            hideSplashButton.setTranslateX(stageShrinker.getWidth()*0.5 - 45.0);
            hideSplashButton.setTranslateY(dispBottom + 15.0);
        }
    }
    
    private void initSplashPane(){   
        applicationTitle.setText("Shrink Piece...");
        applicationTitle.setFont(Font.font("Cooper Black", 24.0));
        
        copyrightDisplay.getStyleClass().add("colThr"); //To comply with ScrollPane CSS-Styles.  
        copyrightDisplay.setEditable(false);
        copyrightDisplay.setMaxWidth(525.0);
        StringBuilder licContent = new StringBuilder();
        //Read licensefile:
        try (InputStreamReader fR = new InputStreamReader(
                getClass().getResourceAsStream("sources/gplv3.txt"));
                BufferedReader licReader = new BufferedReader(fR)) {
            String licLine = licReader.readLine();            
            while(licLine != null){
                licContent.append(licLine).append("\n");
                licLine = licReader.readLine();
            }
        } catch (IOException ex) {
            System.out.println("Exception for reading license file: " + ex);
            licContent = new StringBuilder();
            licContent.append(GPL3_TEXT);
        }
        copyrightDisplay.setText(licContent.toString());
        moveCopyrightViewer();
        
        hideSplashButton.setText("I accept this license");
        hideSplashButton.setOnAction((press) -> {
                shrinkScene.setRoot(shrinkPane);
                splashPane = null;
            });
        splashPane.getChildren().addAll(copyrightDisplay, hideSplashButton, applicationTitle);
    }
    
    private void initImportArrow(){        
        Bounds bImportArr;
        
        double[] xDASmallArrow = new double[]{4.5, 19.5, 19.5, 38.9837093072324, 38.9837093072324, 21.568669735135927, 21.568669735135927, 5.455886152418998, 5.455886152418998, 23.837937995348284};
        double[] yDASmallArrow = new double[]{4.5, 4.5, 4.5, 18.6557434149982, 18.6557434149982, 31.30851029102257, 31.30851029102257, 31.30851029102257, 31.30851029102257, 17.953167875135136};
        double[] xDABigArrow = new double[]{4.5, 42.61823710509185, 42.61823710509185, 42.61823710509185, 42.61823710509185, 86.57480769278612, 86.57480769278612, 42.862876368024956, 42.862876368024956, 42.862876368024956, 42.862876368024956, 5.499931214481933, 5.499931214481933, 23.912848393335764};
        double[] yDABigArrow = new double[]{20.539029092286228, 20.539029092286228, 20.539029092286228, 4.5, 4.5, 36.43631791722947, 36.43631791722947, 68.19489500591817, 68.19489500591817, 49.46733491994365, 49.46733491994365, 49.46733491994365, 49.46733491994365, 36.089567524865856};
        double[] xDAImportArrCanvC = new double[]{33.51723625709383, 4.5};
        double[] yDAImportArrCanvC = new double[]{4.5, 4.5};
        double[] xDAImportArrCanvD = new double[]{16.5, 4.5};
        double[] yDAImportArrCanvD = new double[]{4.5, 4.5};
        double[] xDAImportArrCanvE = new double[]{34.5, 4.5};
        double[] yDAImportArrCanvE = new double[]{4.5, 4.5};
        double[] xDAImportArrCanvF = new double[]{15.5, 4.5};
        double[] yDAImportArrCanvF = new double[]{4.5, 4.5};          
               
        xOffDSArr = 7.0; yOffDSArr = 3.0;
        Canvas smallArrow = new Canvas(43.4837093072324, 35.80851029102257);
        GraphicsContext grConSmallArrow = smallArrow.getGraphicsContext2D();
        grConSmallArrow.setFill(Color.valueOf("0x69a4c0ff"));
        grConSmallArrow.setStroke(Color.valueOf("0x1785b9ff"));
        grConSmallArrow.setLineWidth(3.0);
        grConSmallArrow.setLineJoin(StrokeLineJoin.ROUND);
        grConSmallArrow.fillPolygon(xDASmallArrow, yDASmallArrow, 10);
        grConSmallArrow.strokePolygon(xDASmallArrow, yDASmallArrow, 10);
        dropShadowArrow = new DropShadow(); 
        dropShadowArrow.setOffsetY(yOffDSArr);
        dropShadowArrow.setOffsetX(xOffDSArr);
        smallArrow.setEffect(dropShadowArrow);
        smallArrow.setTranslateX(314.0);
        smallArrow.setTranslateY(292.0);
        Canvas bigArrow = new Canvas(91.07480769278612, 72.69489500591817);
        GraphicsContext grConBigArrow = bigArrow.getGraphicsContext2D();
        grConBigArrow.setFill(Color.valueOf("0x69a4c0ff"));
        grConBigArrow.setStroke(Color.valueOf("0x1785b9ff"));
        grConBigArrow.setLineWidth(3.0);
        grConBigArrow.setLineJoin(StrokeLineJoin.ROUND);
        grConBigArrow.fillPolygon(xDABigArrow, yDABigArrow, 14);
        grConBigArrow.strokePolygon(xDABigArrow, yDABigArrow, 14);
        dropShadowArrow.setOffsetY(4.0);
        dropShadowArrow.setOffsetX(7.0);
        bigArrow.setEffect(dropShadowArrow);
        bigArrow.setTranslateX(347.5);
        bigArrow.setTranslateY(275.4609709077138);
        Canvas importArrCanvC = new Canvas(38.01723625709383, 9.0);
        GraphicsContext grConImportArrCanvC = importArrCanvC.getGraphicsContext2D();
        grConImportArrCanvC.setStroke(Color.valueOf("0x1785b9ff"));
        grConImportArrCanvC.setLineWidth(3.0);
        grConImportArrCanvC.setLineJoin(StrokeLineJoin.ROUND);
        grConImportArrCanvC.strokePolyline(xDAImportArrCanvC, yDAImportArrCanvC, 2);
        importArrCanvC.setTranslateX(277.9827637429062);
        importArrCanvC.setTranslateY(292.0);
        Canvas importArrCanvD = new Canvas(21.0, 9.0);
        GraphicsContext grConImportArrCanvD = importArrCanvD.getGraphicsContext2D();
        grConImportArrCanvD.setStroke(Color.valueOf("0x1785b9ff"));
        grConImportArrCanvD.setLineWidth(3.0);
        grConImportArrCanvD.setLineJoin(StrokeLineJoin.ROUND);
        grConImportArrCanvD.strokePolyline(xDAImportArrCanvD, yDAImportArrCanvD, 2);
        importArrCanvD.setTranslateX(252.0);
        importArrCanvD.setTranslateY(292.0);
        Canvas importArrCanvE = new Canvas(39.0, 9.0);
        GraphicsContext grConImportArrCanvE = importArrCanvE.getGraphicsContext2D();
        grConImportArrCanvE.setStroke(Color.valueOf("0x1785b9ff"));
        grConImportArrCanvE.setLineWidth(3.0);
        grConImportArrCanvE.setLineJoin(StrokeLineJoin.ROUND);
        grConImportArrCanvE.strokePolyline(xDAImportArrCanvE, yDAImportArrCanvE, 2);
        importArrCanvE.setTranslateX(277.0);
        importArrCanvE.setTranslateY(318.0);
        Canvas importArrCanvF = new Canvas(20.0, 9.0);
        GraphicsContext grConImportArrCanvF = importArrCanvF.getGraphicsContext2D();
        grConImportArrCanvF.setStroke(Color.valueOf("0x1785b9ff"));
        grConImportArrCanvF.setLineWidth(3.0);
        grConImportArrCanvF.setLineJoin(StrokeLineJoin.ROUND);
        grConImportArrCanvF.strokePolyline(xDAImportArrCanvF, yDAImportArrCanvF, 2);
        importArrCanvF.setTranslateX(252.0);
        importArrCanvF.setTranslateY(317.0);

        importArr.getChildren().addAll(smallArrow, bigArrow, importArrCanvC, importArrCanvD, importArrCanvE, importArrCanvF);
        
        schermafdrukHulp.setText("No images found on your clipboard (Ctrl + V).\n" + 
                                 "You can use the 'PrtSc' key to add your screen to\n" +
                                 "your clipboard.");
        schermafdrukHulp.setFont(tooltipFont);
        schermafdrukHulp.setVisible(false);
        schermafdrukHulp.setTranslateX(sPiecePane.getBoundsInParent().getMinX() + 50.0);
        schermafdrukHulp.setTranslateY(sPiecePane.getBoundsInParent().getMinY() + 50.0);       
        //Transparent and mouse transparent rectangle surrounding group at base:
        bImportArr = importArr.getBoundsInLocal();
        boundsImportArr = new Rectangle(bImportArr.getMinX(),bImportArr.getMinY(),
                bImportArr.getWidth(), bImportArr.getHeight());
        boundsImportArr.setFill(Color.TRANSPARENT);
        boundsImportArr.setStroke(Color.TRANSPARENT);
        boundsImportArr.setMouseTransparent(true);
        importArr.getChildren().add(boundsImportArr);
        
        importArr.setTranslateX(100.0 - bImportArr.getMinX());//-133.78739929199219);
        importArr.setTranslateY(50.0 - bImportArr.getMinY());//-234.0779266357422);
        
        importArr.setOnMouseEntered((mE)->{            
            dropShadowArrow.setOffsetX(xOffDSArr + 2.0);
            dropShadowArrow.setOffsetY(yOffDSArr + 1.0);
        });
        importArr.setOnMouseExited((mEx)->{
            dropShadowArrow.setOffsetX(xOffDSArr);
            dropShadowArrow.setOffsetY(yOffDSArr);
        });
        importArr.setOnMousePressed((mP)->{
            bigArrow.setEffect(null);
            smallArrow.setEffect(null);
            importArr.setTranslateX(importArr.getTranslateX() + xOffDSArr);
            importArr.setTranslateY(importArr.getTranslateY() + yOffDSArr);
            
            schermafdrukHulp.setVisible(false);
            
            importClipboardImage();            
        });
        importArr.setOnMouseReleased((mR)->{
            bigArrow.setEffect(dropShadowArrow);
            smallArrow.setEffect(dropShadowArrow);
            importArr.setTranslateX(importArr.getTranslateX() - xOffDSArr);
            importArr.setTranslateY(importArr.getTranslateY() - yOffDSArr);
        });
        SPTooltip importGuide = new SPTooltip("Imports (Ctrl + V) clipboard images (generated by 'PrtSc').");
        Tooltip.install(importArr, importGuide);
        //End of imported canvii and functions.    
    }
    
    private void moveCopyrightNotice(){
        if(copyrightNotice != null){
            copyrightNotice.setTranslateX(stageShrinker.getWidth() - 205.0);
            copyrightNotice.setTranslateY(stageShrinker.getHeight() - 60.0);  
        }
    }
    
    private void initCopyrightNotice(){
        moveCopyrightNotice();
        copyrightNotice.setTooltip(new SPTooltip(GPL3_TEXT));
        copyrightNotice.setOnMouseClicked((mC) -> {
            switch(mC.getButton()){
                case PRIMARY:
                    if(copyrightNotice.getTooltip().isShowing()){
                        copyrightNotice.getTooltip().hide();
                    } else {                        
                        double xPosNot = copyrightNotice.getTranslateX();
                        double yPosNot = copyrightNotice.getTranslateY();
                        copyrightNotice.getTooltip().show(shrinkComponents, 
                                xPosNot - 300.0, yPosNot - 170.0);
                    }
                    break;
            }
        });
    }
    
    private void initClearButton(){          
        clearButton.setDisable(true);
        clearButton.setText("Remove the blanks");
        clearButton.setPrefSize(140.0, 25.0);
        clearButton.setTranslateX(sPiecePane.getTranslateX() + 
                sPiecePane.getPrefWidth() -
                clearButton.getPrefWidth());
        clearButton.setTranslateY(sPiecePane.getTranslateY() - 
                clearButton.getPrefHeight());
        clearButton.setOnAction((acCB)->{
            clearButtonAction();        
        });    
    }
    
    private void clearButtonAction(){
        clearButton.setDisable(true);
        clearClearingAreas(); 
        formerPh = ph;
        ph = ShrinkPhase.INIT_REFACT;
        sPiecePaneInteraction.setVisible(true);
    }
    
    public void clearAreaSurroundingSheet(){
        if (pReader != null){
            int nHorPixs, nVerPixs, minX, maxX;
            ArrayList<int[]> clRectangles;
            int[] kBounds;
            int nChainPapRow, nNonPaper, nNonPaperVert, nPaperThr, cSet, lastX, 
                    lastY, hThr, vThr;
            
            formerPh = ph;
            ph = ShrinkPhase.INIT_CROP;
            sPiecePaneIntWasVisible = sPiecePaneInteraction.isVisible();
            sPiecePaneInteraction.setVisible(false);
            //mask.setVisible(false);
            paperRectangle.setVisible(true);
            papRectInt.setVisible(true);
            
            nHorPixs = (int) sPContImage.getWidth();
            nVerPixs = (int) sPContImage.getHeight();
            clRectangles = new ArrayList<int[]>();
            kBounds = new int[4];
            cSet = 0;
            minX = 0;
            nChainPapRow = 0;
            nNonPaper = 0;
            nNonPaperVert = 0;
            maxX = nHorPixs;
            nPaperThr = 10;
            hThr = 20;
            vThr = 20;
            lastX = 0;
            lastY = 0;            
                        
            if ((nVerPixs > 50) && (nHorPixs > 50)){
                for (int vIndCent = ((nVerPixs/2)-25); vIndCent < ((nVerPixs/2) + 25); vIndCent++){
                    nChainPapRow = 0;
                    nNonPaper = 0;
                    for (int hIndCent = ((nHorPixs/2)-25); hIndCent < ((nHorPixs/2) + 25); hIndCent++){
                        if(pReader.getColor(hIndCent, vIndCent).getBrightness() >=
                                        papColBox.getColorThreshold().getBrightness()){
                            lastX = hIndCent;
                            lastY = vIndCent;
                            
                            cSet = 0;
                            kBounds[0] = lastX;
                            for (int hInd = lastX; hInd >= 0; hInd--){
                                if(pReader.getColor(hInd, lastY).getBrightness() <
                                                papColBox.getColorThreshold().getBrightness()){
                                    if (cSet == 0){
                                        kBounds[0] = hInd;
                                    }
                                    cSet++;                        
                                } else {
                                    if ((cSet > 0) && (cSet < hThr)){
                                        cSet = 0;
                                    }
                                }
                            }
                            cSet = 0;
                            kBounds[2] = lastX;
                            for (int hInd = lastX; hInd < nHorPixs; hInd++){
                                if(pReader.getColor(hInd, lastY).getBrightness() <
                                                papColBox.getColorThreshold().getBrightness()){
                                    if (cSet == 0){
                                        kBounds[2] = hInd;
                                    }
                                    cSet++;                        
                                } else {
                                    if ((cSet > 0) && (cSet < hThr)){
                                        cSet = 0;
                                    }
                                }
                            }
                            cSet = 0;
                            kBounds[1] = lastY;
                            for (int vInd = lastY; vInd >= 0; vInd--){
                                if(pReader.getColor(lastX, vInd).getBrightness() <
                                                papColBox.getColorThreshold().getBrightness()){
                                    if (cSet == 0){
                                        kBounds[1] = vInd;
                                    }
                                    cSet++;                        
                                } else {
                                    if ((cSet > 0) && (cSet < vThr)){
                                        cSet = 0;
                                    }
                                }
                            }
                            cSet = 0;
                            kBounds[3] = lastY;
                            for (int vInd = lastY; vInd < nVerPixs; vInd++){
                                if(pReader.getColor(lastX, vInd).getBrightness() <
                                                papColBox.getColorThreshold().getBrightness()){
                                    if (cSet == 0){
                                        kBounds[3] = vInd;
                                    }
                                    cSet++;                        
                                } else {
                                    if ((cSet > 0) && (cSet < vThr)){
                                        cSet = 0;
                                    }
                                }
                            }
                            if (((kBounds[2] - kBounds[0]) >= (0.9*boundsMBounds.getWidth())) &&
                                    ((kBounds[3] - kBounds[1]) >= (0.9*boundsMBounds.getHeight()))){
                                vIndCent = nVerPixs;
                                break;
                            }                            
                        }
                    }
                }                     
            } else {
                kBounds[0] = 0; kBounds[1] = 0;
                kBounds[2] = (nHorPixs -1); kBounds[3] = (nVerPixs - 1); 
            }
            //Set paper color to non-paper parts:
            paperRectangle.setTranslateX((double)kBounds[0]);
            paperRectangle.setTranslateY((double)kBounds[1]);
            paperRectangle.setWidth(((double)kBounds[2] - (double)kBounds[0]));
            paperRectangle.setHeight(((double)kBounds[3] - (double)kBounds[1]));            
        }  
    }
    
    private void cropImage(){
        if ((piece != null) && (sPCont != null) && (paperRectangle != null)){
            double scaleFact, newH, newW;
            
            generateSPGraphicsRecovery();
            
            formerPh = ph;
            ph = ShrinkPhase.CROP;            
            if (papRectInt != null){
                papRectInt.setVisible(false);
            }
            mouseExitPaperFrame();
            paperRectangle.setVisible(false);
            clearShrunkPieceContext();            
            boundsPaperRectangle = paperRectangle.getBoundsInParent();
            scaleFact = boundsMBounds.getWidth()/boundsPaperRectangle.getWidth();
            newH = boundsPaperRectangle.getHeight()*scaleFact;
            newW = boundsMBounds.getWidth() - (marchBounds.getStrokeWidth()*2.0);
            shrunkPiece.setWidth(newW);
            shrunkPiece.setHeight(newH);
            sPCont.drawImage(piece, boundsPaperRectangle.getMinX(), boundsPaperRectangle.getMinY(),
                    boundsPaperRectangle.getWidth(), 
                    boundsPaperRectangle.getHeight(), 0.0, 0.0,
                    newW,newH);             
            storeShrunkPieceGraphics();
            
            adjustWidthPiecePaneToMBounds();
        }        
    }
    
    private void undoCropImage(){
        setInitWidthSPiecePane();
        recoverShrunkPieceGraphics();
        clearAreaSurroundingSheet();
    }
    
    public void setClearAreas(){
        if (pReader != null) {
            int xMin, xMax, yMin, yMax, cSet, nDarkH, nDarkV, yC, yCA;
            int hThrA = 4;
            int vThrA = 1;
            int[] cABounds;
                                   
            progIndicator.setVisible(true);
            
            xMin = 0;
            xMax = (int) sPContImage.getWidth();
            yMin = 0;
            yMax = (int) sPContImage.getHeight();
            
            clearAreas.getChildren().clear(); 
            yCA = 0;
            for(yC = yMin; yC <= yMax; yC++){
                nDarkV = 0;
                cABounds = new int[4];
                cABounds[0] = xMin;
                cABounds[1] = yC;
                for(yCA = yC; yCA < yMax; yCA++){
                    nDarkH = 0;
                    for(int xCA = xMin; xCA < xMax; xCA++){  
                        if (pReader.getColor(xCA, yCA).getBrightness() <
                                papColBox.getColorThreshold().getBrightness()){
                            nDarkH++;  
                            if(nDarkH > hThrA){
                                break;
                            }
                        }
                    }
                    if(nDarkH > hThrA){
                        nDarkV++;
                    }
                    if ((nDarkH <= hThrA) && (nDarkV <= vThrA)){
                        cABounds[2] = xMax;
                        cABounds[3] = yCA;                        
                    } else {                        
                        break;
                    }
                }
                if ((cABounds[3] - cABounds[1]) > 0){
                    addClearingArea(cABounds);
                }
                yC = yCA;                                
            }
            if (clearAreas.getChildren().size() > 0){
                formerPh = ph;
                ph = ShrinkPhase.SHRINK;
                if(sPiecePaneInteraction.isVisible()){
                    sPiecePaneInteraction.setVisible(false);
                }
                clearButton.setDisable(false); 
            } else {
                clearButtonAction();
                //mask.setVisible(true);
            }
            
            progIndicator.setVisible(false);
        }
    }
    
    private void discardClearAreas(){
        if (clearAreas.getChildren().size() > 0){
            clearAreas.getChildren().clear();
            clearButton.setDisable(true); 
        }  
    }
    
    private void escapeClearing(){
        discardClearAreas();           
        clearButtonAction();
    }
    
    private void addClearingArea(int[] boundsArea){
        if((sPCont != null) && (clearAreas != null)){
            ClearArea nAr = new ClearArea();
            nAr.setTranslateXRA((double) boundsArea[0]);
            nAr.setTranslateYRA((double) boundsArea[1]);
            nAr.setWidthExclusive((double) (boundsArea[2]-boundsArea[0]));
            nAr.setHeightExclusive((double) (boundsArea[3]-boundsArea[1]));
            nAr.setOnMousePressed((mP)->{
                if(mP.getButton().equals(MouseButton.SECONDARY)){
                    showShrinkContextMenu(nAr, mP.getScreenX(), mP.getScreenY());
                }
            });
            clearAreas.getChildren().add(nAr);            
        }
    }
    
    private void deleteClearingArea(ClearArea areaToRemove){
        if(clearAreas != null){
            indAreaToRemove = clearAreas.getChildren().indexOf(areaToRemove);
            if(indAreaToRemove >= 0){
                removedClearArea = areaToRemove;
                clearAreas.getChildren().remove(indAreaToRemove);
            }
        }
    }
    
    private void restoreDeletedClearArea(){
        if(removedClearArea!= null){
            if((indAreaToRemove >= 0) && 
                    (indAreaToRemove < clearAreas.getChildren().size())){
                clearAreas.getChildren().add(indAreaToRemove, removedClearArea);
            } else {
                clearAreas.getChildren().add(removedClearArea);
            }
        }
    }
    
    private void clearClearingAreas(){
        if((pWriter != null) && (pReader != null) && (clearAreas != null)){
            int nCAs;
                    
            nCAs = clearAreas.getChildren().size();
            if(nCAs > 0){
                ClearArea currCA;
                ClearArea nextCA;
                double widthSum, heightSum, pY, pH, papBrightness;
                    
                generateSPGraphicsRecovery();

                widthSum = sPContImage.getWidth();
                heightSum = 0.0;
                pY = 0.0; pH = 0.0;
                papBrightness = papColBox.getColorThreshold().getBrightness();
                
                clearShrunkPieceContext();
                pY = ((ClearArea)clearAreas.getChildren().get(0)).getTranslateYRA();
                if(pY > 0.0){
                    pH = pY;  
                    sPCont.drawImage(sPContImage,0.0,0.0,
                            sPContImage.getWidth(),
                            pH, 0.0, heightSum, sPContImage.getWidth(), pH);
                    heightSum += pH;                    
                }
                for(int cAInd = 0; cAInd < nCAs; cAInd++){
                    currCA = ((ClearArea)clearAreas.getChildren().get(cAInd));
                    pY = currCA.getTranslateYRA() + currCA.getHeightExclusive();
                    if ((cAInd+1) < nCAs){
                        nextCA = ((ClearArea)clearAreas.getChildren().get(cAInd + 1));  
                        pH = nextCA.getTranslateYRA() - pY;
                    } else {
                        nextCA = null;
                        pH = ((int) sPContImage.getHeight()) - pY;
                    }
                    sPCont.drawImage(sPContImage, currCA.getTranslateXRA(),
                            pY, currCA.getWidthExclusive(), pH,
                            0.0, heightSum, currCA.getWidthExclusive(), pH);
                    heightSum += pH;
                }
                clearAreas.getChildren().clear();
                shrunkPiece.setWidth(widthSum);
                shrunkPiece.setHeight(heightSum);
                
                storeShrunkPieceGraphics();
                for(int yCI = 0; yCI < heightSum; yCI++){
                    for(int xCI = 0; xCI < widthSum; xCI++){
                        if(pReader.getColor(xCI, yCI).getBrightness() >= papBrightness){
                            pWriter.setColor(xCI, yCI, Color.TRANSPARENT);
                        }
                    }
                }  
            }
        }
    }
    
    public static void updatePaperFrameGuides(){
        if(layGuidesInit != null){
            for(Node lG : layGuidesInit.getChildren()){
                ((GuideRectangle)lG).takePosition();
            }
        }
    }
    private void mouseEnterPaperFrame(){
        layGuidesInit.setVisible(true);
        paperRectangle.setStrokeWidth(2.5);
    }
    private void mouseExitPaperFrame(){   
        if (layGuidesInit != null){
            layGuidesInit.setVisible(false);
        }
        paperRectangle.setStrokeWidth(1.0);
    }
    
    private void fadeMarchPerifery(Bounds newBounds){
        Bounds sPieceContentBnds;
        
        sPieceContentBnds = newBounds;
        if (marchPerifShade.getPoints().size() > 0){
            marchPerifShade.getPoints().clear();
        }
        marchPerifShade.getPoints().add(marchBounds.getBoundsInParent().getMaxX());
        marchPerifShade.getPoints().add(0.0);
        marchPerifShade.getPoints().add(sPieceContentBnds.getMaxX());
        marchPerifShade.getPoints().add(0.0);
        marchPerifShade.getPoints().add(sPieceContentBnds.getMaxX());
        marchPerifShade.getPoints().add(sPieceContentBnds.getMaxY());
        marchPerifShade.getPoints().add(0.0);
        marchPerifShade.getPoints().add(sPieceContentBnds.getMaxY());
        marchPerifShade.getPoints().add(0.0);
        marchPerifShade.getPoints().add(marchBounds.getBoundsInParent().getMaxY());
        marchPerifShade.getPoints().add(marchBounds.getBoundsInParent().getMaxX());
        marchPerifShade.getPoints().add(marchBounds.getBoundsInParent().getMaxY());        
    }
    
    private void updateSPPaneInteractionSize(Bounds newPaneContents){
        sPiecePaneInteraction.setTranslateX(marchBounds.getBoundsInParent().getMinX());
        sPiecePaneInteraction.setTranslateY(marchBounds.getBoundsInParent().getMinY());
        sPiecePaneInteraction.setWidth(newPaneContents.getWidth() - 
                marchBounds.getBoundsInParent().getMinX());
        sPiecePaneInteraction.setHeight(newPaneContents.getHeight() - 
                marchBounds.getBoundsInParent().getMinY());   
    }
    
    
    private void showShrinkContextMenu(Node forNode, double atScreenX, double andScreenY){
        menuButton.setTranslateX(atScreenX);
        menuButton.setTranslateY(andScreenY);
        selItem = forNode;
        if (selItem.getClass().getSimpleName().equals("Polygon")){
            clearAccordingToArea.setVisible(true);
        } else {
            clearAccordingToArea.setVisible(false);
        }
        shrinkMenu.show(forNode, atScreenX, andScreenY);
    }
    
    private void initMaskBrush(){
        maskGr = new Group();
        brush = new Canvas(50.0, 74.0);
        
        double[] xDAMaskGrCanv = new double[]{1.5, 1.5318587171504419, 1.627394269487354, 1.7864849114051253, 2.008927905953385, 2.294439783194207, 2.6426567014420357, 3.053134910924655, 3.5253513192751598, 4.058704158134049, 4.652513750011963, 5.306023374435711, 6.018400232274132, 6.788736507014505, 7.616050521637305, 8.499287989615027, 9.437323358441006, 10.428961243975493, 11.472937953782093, 12.567923097512335, 13.712521282287184, 14.905273890913861, 16.144660940672793, 17.429103020305604, 18.756963302735926, 20.126549630957584, 21.536116674432833, 22.983868153252047, 24.467959127220354, 25.986498346955273, 27.537550663997422, 29.119139496864648, 30.729249349905956, 32.365828381745814, 34.02679102004538, 35.710020619249065, 37.41337215792885, 39.13467497228959, 40.871735522351514, 42.622340187285204, 44.38425808633616, 46.155243921744784, 47.933040840038814, 49.7153833080514, 51.500000000000455, 53.28461669194945, 55.066959159962096, 56.844756078256125, 58.61574191366475, 60.37765981271565, 62.12826447764934, 63.865325027711265, 65.586627842072, 67.28997938075173, 68.97320897995542, 70.63417161825498, 72.27075065009484, 73.88086050313615, 75.46244933600337, 77.01350165304547, 78.53204087278039, 80.01613184674869, 81.46388332556785, 82.8734503690431, 84.24303669726476, 85.57089697969502, 86.85533905932783, 88.09472610908671, 89.28747871771338, 90.43207690248818, 91.52706204621842, 92.57103875602496, 93.56267664155945, 94.50071201038537, 95.38394947836315, 96.21126349298589, 96.98159976772621, 97.69397662556463, 98.34748624998838, 98.94129584186624, 99.47464868072507, 99.94686508907557, 100.35734329855813};
        double[] yDAMaskGrCanv = new double[]{1.5, 2.856308685881288, 4.210888961570845, 5.562014619474326, 6.907963854384889, 8.24702145766355, 9.577481003013133, 10.897647021060209, 12.205837159974408, 13.500384329371002, 14.779638824765811, 16.04197042987346, 17.285770494071755, 18.50945398238315, 19.711461495362244, 20.89026125631426, 22.04435106331283, 23.172260203528708, 24.2725513274313, 25.343822280472466, 26.384707889920946, 27.393881704568003, 28.37005768508891, 29.311991842905684, 30.21848382546193, 31.08837844589084, 31.92056715512581, 32.71398945457878, 33.46763424758501, 34.18054112789275, 34.85180160355583, 35.480560254669115, 36.06601582347179, 36.607422235429, 37.104089549991045, 37.5553848398182, 37.96073299735099, 38.319617467697356, 38.631580906904105, 38.89622576477245, 39.11321479147546, 39.28227146733212, 39.40318035518965, 39.475787374965705, 39.5, 39.47578737496565, 39.40318035518959, 39.282271467332066, 39.1132147914754, 38.89622576477234, 38.63158090690399, 38.319617467697185, 37.96073299735076, 37.55538483981803, 37.10408954999082, 36.60742223542877, 36.0660158234715, 35.48056025466883, 34.85180160355549, 34.18054112789241, 33.46763424758467, 32.71398945457844, 31.92056715512541, 31.088378445890385, 30.218483825461476, 29.31199184290523, 28.370057685088454, 27.39388170456749, 26.384707889920435, 25.343822280471954, 24.272551327430733, 23.172260203528197, 22.044351063312263, 20.890261256313693, 19.711461495361675, 18.50945398238258, 17.285770494071187, 16.04197042987289, 14.779638824765186, 13.500384329370377, 12.205837159973726, 10.897647021059583, 9.577481003012508};
        double[] xDAMaskGrCanvB = new double[]{9.128758688929338, 8.39925868027035, 7.704345937165442, 7.044459381397587, 6.420015811458995, 5.831409639292417, 5.279012641172017, 4.763173722881447, 4.284218699337828, 3.8424500887995805, 3.43814692178978, 3.0715645648542704, 2.742934559266814, 2.4524644747825164, 2.200337778532628, 1.986713719142358, 1.8117272261462745, 1.6754888247639883, 1.5780845660898422, 1.519575972741336, 1.5, 1.519369012469781, 1.5776707762673823, 1.6748684667491602, 1.810900691770371, 1.9856815304622728, 2.1991005875008227, 2.451023062835418, 2.7412898368310152, 3.069717570771786, 3.436098822661677, 3.8402021782491715, 4.2817723971934925, 4.760530574280153, 5.276174315582978, 5.828377929462988, 6.4167926322821245, 7.041046768702245, 7.700746046430936, 8.395473785264869, 9.124791180273746, 9.888237578959092, 10.68533077221224, 11.515567298888016, 12.378422763801382, 13.273352168946644, 14.199790257730228, 15.157151871998451, 16.144832321636386, 17.162207766502547, 18.20863561046025, 19.283454907254622, 20.385986777980804, 21.51553483987834, 22.67138564618125, 23.852809136746316, 25.059059099175215, 26.2893736401374, 27.542975666599148, 28.81907337665143, 30.116860759629674, 31.435518105206654, 32.77421252113959, 34.13209845934267, 35.508318249953106, 36.90200264305423, 38.31227135771189, 39.73823363797902, 41.17898881551622, 42.63362687847308, 44.101229046271214, 45.58086834992565, 47.07161021753859, 48.57251306459483, 50.08262888868654, 51.60100386829242, 53.12667896523135, 54.65869053041189};
        double[] yDAMaskGrCanvB = new double[]{92.87180348806226, 91.4383064607128, 89.98783026234804, 88.5212910448235, 87.03961510574192, 85.54373830338375, 84.0346054655991, 82.5131697930342, 80.9803922570693, 79.43724099284879, 77.88469068778613, 76.32372196593047, 74.75532076858332, 73.18047773155712, 71.60018755946834, 70.01544839746066, 68.42726120075514, 66.83662910242538, 65.2445567797975, 63.65204981987392, 62.06011408418328, 60.46975507345695, 58.881977292532895, 57.29778361588899, 55.71817465420611, 54.14414812236072, 52.57669820924724, 51.016814949826426, 49.46548359979863, 47.92368401329486, 46.39239002397977, 44.872568829958084, 43.36518038287073, 41.871176781569375, 40.3915016707503, 38.927089644927776, 37.47886565812473, 36.04774443965164, 34.63462991634384, 33.24041464162207, 31.865979231736617, 30.51219180955127, 29.179907456218018, 27.869967671089967, 26.583199840211762, 25.32041671372525, 24.08241589251884, 22.869979324445694, 21.683872810428966, 20.524845520765098, 19.39362952193227, 18.290939314200955, 17.217471380340953, 16.17390374570732, 15.160895549985185, 14.179086630863253, 13.229097119898313, 12.31152705082701, 11.42695598057162, 10.575942623179458, 9.759024496927282, 8.976717584812775, 8.229516008649284, 7.517891716967256, 6.842294186921947, 6.203150140393689, 5.600863274461176, 5.035814006418093, 4.508359233493081, 4.018832107425794, 3.5675418240413137, 3.1547734279553197, 2.7807876325338157, 2.4458206552209845, 2.1500840683390834, 1.893764665455194, 1.6770243433978749, 1.5};
        double[] xDAMaskGrCanvC = new double[]{6.1128411659311155, 5.423246549410749, 4.785961725915229, 4.202199803327062, 3.6730720056447694, 3.1995855577047223, 2.7826417678720077, 2.4230343123480225, 2.1214477243630654, 1.8784560911284984, 1.6945219610288405, 1.5699954631348874, 1.5051136407130912, 1.5, 1.554664275101345, 1.6690024094625642, 1.842796753946402, 2.0757164811402617, 2.367318215105115, 2.7170468753668047, 3.1242367335432846, 3.588112680596396, 4.107791702295799, 4.682284560087226, 5.3104976741637415, 5.991235205157352, 6.7232013304873135, 7.505002711031807, 8.33515114342822, 9.212066392952977, 10.134079201588122, 11.09943446554837, 12.106294576221785, 13.152742918161437, 14.23678751747218, 15.35636483364624, 16.50934368762961, 17.69352931864273, 18.90666756203217, 20.14644914020147, 21.41051405845195, 22.69645609736699, 24.00182739318734, 25.324143097458943, 26.660886107083172, 28.009511855765822, 29.367453157743228, 30.732125094566925, 32.100929935642114, 33.47126208315615, 34.84051303198163, 36.20607633511429, 37.56535256519305, 38.915754262657856, 40.2547108611256, 41.57967358060995, 42.88812027926832, 44.1775602544426, 45.445538983853, 46.68964279792095, 47.90750347432612, 49.09680274605233, 50.25527671434048, 51.38072015814845, 52.470990731915094, 53.52401304363633, 54.53778260549183, 55.51036964950168, 56.43992280094949, 57.32467260257903, 58.16293488285709, 58.95311396188936, 59.69370568888809, 60.383300305408454, 61.020585128903974, 61.604347051492084, 62.13347484917449, 62.60696129711448, 63.023905086947195, 63.38351254247118, 63.68509913045614, 63.92809076369082, 64.11202489379042, 64.23655139168432, 64.30143321410617, 64.30654685481926, 64.25188257971791, 64.13754444535664, 63.96375010087286, 63.730830373679, 63.439228639714145, 63.0894999794524, 62.68231012127592, 62.218434174222864, 61.698755152523404, 61.124262294732034, 60.496049180655575, 59.81531164966191, 59.083345524331946, 58.30154414378751, 57.47139571139104, 56.594480461866226, 55.672467653231195, 54.70711238927089, 53.700252278597475, 52.65380393665782, 51.56975933734702, 50.45018202117302, 49.29720316718971, 48.113017536176585, 46.89987929278715, 45.66009771461785, 44.39603279636731, 43.110090757452326, 41.80471946163203, 40.48240375736037, 39.14566074773609, 37.79703499905355, 36.43909369707603, 35.074421760252335, 33.705616919177146, 32.33528477166311, 30.966033822837687, 29.60047051970497, 28.241194289626264, 26.89079259216146, 25.551835993693658, 24.22687327420931, 22.91842657555094, 21.628986600376777, 20.36100787096626, 19.116904056898363, 17.899043380493197, 16.709744108766984, 15.551270140478834, 14.42582669667081, 13.335556122904165, 12.28253381118293, 11.26876424932749, 10.296177205317576, 9.36662405386977, 8.481874252240289, 7.64361197196223, 6.853432892929959};
        double[] yDAMaskGrCanvC = new double[]{93.95558868477053, 93.49313773354896, 92.9445166007036, 92.31076961774238, 91.59310315806727, 90.79288334057367, 89.9116334291657, 88.95103093313674, 87.91290441393494, 86.79923000439311, 85.61212764704669, 84.35385705870345, 83.0268134289438, 81.6335228607411, 80.17663756188142, 78.65893079633514, 77.08329160519116, 75.4527193072035, 73.77031778941688, 72.03928959874196, 70.26292984572484, 68.44461993211712, 66.58782111418532, 64.69606791401281, 62.77296139133654, 60.82216228872454, 58.84738406314477, 56.85238581718869, 54.840965143405924, 52.816950895370496, 50.78419589924107, 48.74656961968702, 46.70795079414262, 44.6722200494097, 42.64325251466437, 40.62491044492788, 38.6210358690451, 36.63544327616398, 34.67191235463878, 32.73418079717851, 30.82593718593614, 28.950813971083164, 27.112380556234143, 25.3141365038839, 23.55950487379164, 21.851825706992088, 20.194349667837173, 18.590231856172295, 17.042525801424063, 15.554177650033637, 14.128020557299237, 12.766769294304368, 11.473015080196717, 10.249220649654944, 9.097715564933424, 8.020691781408345, 7.020199475065681, 6.098143139874651, 5.256277962475735, 4.496206481082481, 3.819375534959363, 3.227073510281002, 2.7204278876154717, 2.3004030957014265, 1.9677986756029782, 1.7232477587377275, 1.5672158616752654, 1.5, 1.5217281229248556, 1.6323588697329114, 1.831681648509857, 2.119317037017481, 2.4947175049455836, 2.9571684561670963, 3.5057895890124655, 4.139536571973736, 4.857203031648851, 5.657422849142449, 6.538672760550412, 7.499275256579381, 8.537401775781063, 9.651076185323006, 10.838178542669425, 12.096449131012605, 13.42349276077232, 14.81678332897502, 16.273668627834695, 17.791375393380918, 19.36701458452484, 20.997586882512564, 22.679988400299123, 24.411016590974043, 26.187376343991218, 28.005686257598995, 29.862485075530742, 31.754238275703244, 33.67734479837952, 35.62814390099152, 37.602922126571286, 39.59792037252731, 41.60934104631008, 43.633355294345506, 45.66611029047499, 47.70373657002898, 49.74235539557344, 51.77808614030636, 53.807053675051634, 55.82539574478818, 57.82927032067096, 59.81486291355202, 61.77839383507717, 63.71612539253755, 65.62436900377992, 67.4994922186329, 69.33792563348192, 71.13616968583216, 72.89080131592436, 74.59848048272403, 76.25595652187889, 77.86007433354376, 79.40778038829194, 80.89612853968248, 82.32228563241682, 83.68353689541163, 84.97729110951934, 86.20108554006117, 87.35259062478258, 88.42961440830771, 89.43010671465044, 90.35216304984141, 91.19402822724038, 91.95409970863363, 92.63093065475675, 93.22323267943511, 93.72987830210064, 94.14990309401469, 94.4825075141132, 94.72705843097839, 94.88309032804085, 94.95030618971617, 94.92857806679132, 94.8179473199832, 94.61862454120626, 94.33098915269869};
        double[] xDAMaskGrCanvD = new double[]{1.5, 1.546294244888486, 1.6850912532696043, 1.9161340050354738, 2.2389946621969443, 2.653075361140907, 3.157609319736139, 3.7516622572372285, 4.434134124357627, 5.203761140307506, 6.059118133024867, 6.9986211782661485, 8.020530532669454, 9.122953855358446, 10.303849712122314, 11.561031355681735, 12.89217077504179, 14.294803006431948, 15.76633069785197, 17.30402891876946, 18.905050206064402, 20.566429836874818, 22.28509131858158, 24.05785208576401, 25.8814293935788, 27.752446396647485, 29.667438402196808, 31.62285928587221, 33.6150880583437, 35.640435570544696, 37.69515134512653, 39.77543052147951, 41.87742090145849, 43.99723008276737, 46.130932666791864, 48.27457752753418, 50.4241951281889, 52.5758048718111, 54.72542247246582, 56.869067333208136, 59.00276991723263, 61.12257909854151, 63.22456947852049, 65.30484865487341, 67.3595644294553, 69.38491194165624, 71.37714071412779, 73.33256159780319, 75.24755360335251, 77.11857060642114, 78.94214791423593, 80.71490868141842, 82.43357016312518, 84.0949497939356, 85.69597108123054, 87.23366930214803, 88.70519699356805, 90.10782922495821, 91.43896864431827, 92.69615028787769, 93.87704614464155, 94.97946946733055, 96.00137882173385, 96.94088186697513, 97.7962388596925, 98.56586587564237, 99.24833774276277, 99.84239068026386, 100.3469246388591, 100.761005337803, 101.08386599496453, 101.3149087467304, 101.45370575511151, 101.5, 101.45370575511151, 101.3149087467304, 101.08386599496453, 100.76100533780306, 100.3469246388591, 99.84239068026386, 99.24833774276277, 98.56586587564237, 97.7962388596925, 96.94088186697513, 96.00137882173385, 94.97946946733055, 93.87704614464155, 92.69615028787774, 91.43896864431827, 90.10782922495821, 88.70519699356805, 87.23366930214809, 85.69597108123054, 84.09494979393565, 82.43357016312518, 80.71490868141842, 78.94214791423599, 77.1185706064212, 75.24755360335251, 73.33256159780319, 71.37714071412779, 69.3849119416563, 67.3595644294553, 65.30484865487347, 63.22456947852049, 61.12257909854151, 59.00276991723268, 56.86906733320819, 54.72542247246582, 52.57580487181116, 50.4241951281889, 48.274577527534234, 46.130932666791864, 43.99723008276737, 41.87742090145855, 39.775430521479564, 37.69515134512659, 35.640435570544696, 33.61508805834376, 31.62285928587221, 29.667438402196808, 27.752446396647485, 25.881429393578856, 24.05785208576407, 22.28509131858158, 20.566429836874818, 18.905050206064402, 17.304028918769518, 15.76633069785197, 14.294803006432005, 12.89217077504179, 11.561031355681735, 10.303849712122314, 9.122953855358446, 8.020530532669454, 6.9986211782661485, 6.059118133024867, 5.203761140307506, 4.4341341243576835, 3.7516622572372285, 3.157609319736139, 2.653075361140907, 2.238994662197001, 1.9161340050354738, 1.6850912532696043, 1.546294244888486};
        double[] yDAMaskGrCanvD = new double[]{24.49467554553746, 23.505164186433262, 22.517485174576223, 21.53346746412808, 20.554933229363428, 19.583694490422204, 18.621549757865182, 17.67028070224717, 16.7316488548729, 15.807392345846324, 14.89922268545422, 14.008821594842857, 13.137837891857998, 12.287884437812977, 11.460535150841793, 10.657322091364222, 9.879732625063411, 9.129206668626239, 8.407134023349613, 7.714851801548207, 7.053641950530505, 6.424728878727024, 5.82927718836811, 5.268389518908862, 4.743104505193969, 4.254394854146199, 3.803165543535897, 3.390252146171406, 3.0164192826101726, 2.6823592052575123, 2.388690516474526, 2.135957023069068, 1.9246267292904804, 1.7550909701936916, 1.6276636869765753, 1.542580845633438, 1.5, 1.5, 1.542580845633438, 1.6276636869765753, 1.755090970193578, 1.9246267292904804, 2.135957023069068, 2.388690516474526, 2.6823592052575123, 3.0164192826101726, 3.390252146171406, 3.803165543535897, 4.254394854146085, 4.743104505193969, 5.268389518908862, 5.82927718836811, 6.424728878727024, 7.053641950530505, 7.714851801548207, 8.407134023349613, 9.129206668626239, 9.879732625063411, 10.657322091364222, 11.460535150841793, 12.287884437812977, 13.137837891857998, 14.008821594842857, 14.89922268545422, 15.80739234584621, 16.7316488548729, 17.67028070224717, 18.621549757865182, 19.58369449042209, 20.554933229363428, 21.53346746412808, 22.517485174576223, 23.505164186433262, 24.49467554553746, 25.48418690464166, 26.4718659164987, 27.455883626946843, 28.43441786171138, 29.40565660065272, 30.36780133320974, 31.319070388827754, 32.25770223620202, 33.1819587452286, 34.0901284056207, 34.98052949623195, 35.851513199216924, 36.701466653261946, 37.52881594023313, 38.3320289997107, 39.10961846601151, 39.860144422448684, 40.58221706772531, 41.274499289526716, 41.93570914054442, 42.5646222123479, 43.16007390270681, 43.72096157216606, 44.246246585880954, 44.734956236928724, 45.186185547539026, 45.59909894490352, 45.97293180846475, 46.30699188581741, 46.6006605746004, 46.853394068005855, 47.06472436178444, 47.23426012088123, 47.36168740409835, 47.446770245441485, 47.48935109107492, 47.48935109107492, 47.446770245441485, 47.36168740409835, 47.234260120881345, 47.06472436178444, 46.853394068005855, 46.6006605746004, 46.30699188581741, 45.97293180846475, 45.59909894490352, 45.186185547539026, 44.73495623692884, 44.246246585880954, 43.72096157216606, 43.16007390270681, 42.5646222123479, 41.93570914054442, 41.274499289526716, 40.58221706772531, 39.860144422448684, 39.10961846601151, 38.3320289997107, 37.52881594023313, 36.701466653261946, 35.851513199216924, 34.980529496232066, 34.0901284056207, 33.18195874522871, 32.25770223620202, 31.319070388827754, 30.36780133320974, 29.405656600652833, 28.434417861711495, 27.455883626946843, 26.4718659164987, 25.48418690464166};
        double[] xDABrush = new double[]{13.49098472608938, 14.30371608859798, 15.143684698712093, 16.009291628441645, 16.89888914579791, 17.810783851345207, 18.743239901682955, 19.694482313721835, 20.662700343465588, 21.64605093286451, 22.64266221818184, 23.650637093192188, 24.66805682043156, 25.692984683622626, 26.72346967432486, 27.75755020578947, 28.793257846951462, 29.828621069450207, 30.861669000545362, 31.890435174785637, 32.91296127728697, 33.9273008714967, 34.93152310434601, 35.923716381738416, 36.90199200737783, 37.86448777800916, 38.80937152822838, 39.734844618113414, 40.63914535703708, 41.520552357145675, 42.37738781011882, 43.20802068097231, 44.010869812826286, 44.78440693672741, 45.5271595807954, 46.23771387315679, 46.914717233331146, 46.914717233331146, 47.64100601707253, 47.89287344091963, 48.06987919397983, 48.17169096644653, 48.198117617355194, 48.14910953342991, 48.02475872222749, 47.82529863940289, 47.55110375042028, 47.20268882753385, 46.78070798335597, 46.28595344282985, 45.71935405590938, 45.08197355374057, 44.37500855161784, 43.5997863024632, 42.75776220504815, 41.850517071634215, 40.87975416016309, 39.84729597656815, 38.75508085320956, 37.60515930985815, 36.39969020405863, 35.140936678100104, 33.831261910202954, 32.47312467789874, 31.06907474193207, 29.62174805935149, 28.133861834775303, 26.60820941912391, 25.04765506539553, 23.455128551330006, 21.833619679057733, 20.18617266205831, 18.51588040996762, 18.51588040996762, 18.619234403576343, 17.864786307960458, 16.425371089740395, 14.63074142857647, 12.892025352209544, 11.607541759934918, 11.071550363037318, 11.406840331149567, 12.536600773259181, 14.202017187168167, 16.02156274289348, 17.57840148151945, 17.57840148151945, 18.250435082621152, 18.559364045638006, 18.83676406470545, 19.07371927496507, 19.26261374099181, 19.397376239224627, 19.473675392087387, 19.473675392087387, 19.400018542282965, 19.18086166896495, 18.82160114169443, 18.331083146970116, 17.72138586575801, 17.007522068590845, 16.207069451332927, 15.339737813971112, 14.426883739925984, 13.49098472608938, 13.49098472608938, 14.175025012740718, 14.49098472608938, 14.776559945462452, 15.023073612327323, 15.223035533658276, 15.37036996766119, 15.46060023211379, 15.49098472608938, 15.49098472608938, 15.450068636297544, 15.328436451191294, 15.12940597970885, 14.858406254278577, 14.522829440966575, 14.13182920045665, 13.696071000048278, 13.227441187511772, 13.227441187511772, 12.907904704332566, 12.768268094301959, 12.651656340504758, 12.563916843204595, 12.509449227164453, 12.49098472608938, 12.49098472608938, 12.084248083013563, 11.903199473796917, 11.747839900612007, 11.62495932230496, 11.539928209794198, 11.4964628307211, 11.4964628307211, 9.805528823326028, 8.163239908647938, 6.616841748654167, 5.2108214028877455, 3.9856275167615536, 2.9765066876745436, 2.2124894845778726, 1.7155552913620227, 1.5, 1.5, 1.610693941661907, 1.9400501144318696, 2.479958676265767, 3.2171252865175006, 4.133398456726923, 5.206216500507139, 6.409163077141443, 7.712617649562162, 9.08448484027872, 10.49098472608938, 10.49098472608938, 9.707827572769077, 8.938070455474247, 8.194884131898846, 7.49098472608938, 6.838416152037041, 6.248344038970117, 5.730864684341952, 5.294832303382748, 4.947707531021649, 4.695429768354984, 4.542315557846507, 4.542315557846507, 4.647881456086907, 4.961371584408994, 5.473260703897381, 6.16799532268476, 7.024466280931961, 8.016650141967943, 9.11439990124427, 10.284360987813614, 11.49098472608938, 11.49098472608938, 10.90571376004101, 10.342934428994113, 9.824274027030583, 9.36966438252972, 8.99657588918177, 8.719346128555514, 8.54862888487969, 8.49098472608938, 8.49098472608938, 9.175025012740718, 9.49098472608938, 9.776559945462452, 10.023073612327323, 10.223035533658276, 10.37036996766119, 10.46060023211379, 10.49098472608938, 10.49098472608938, 9.326175384209364, 8.186191931167457, 7.0953311350828585, 6.076842799925828, 5.152434236311478, 4.341807607861142, 3.6622400137906084, 3.1282152575361124, 2.7511151496299817, 2.5389769241697877, 2.5389769241697877, 2.6821274966129067, 3.1074610326066363, 3.8027414607233254, 4.747966828939866, 5.915944724380211, 7.2730745495245515, 8.78031415022349, 10.39430298738921, 12.068609540758473, 12.068609540758473, 11.100065155981326, 10.160949502228561, 9.279797133423926, 8.48338141840901, 7.795901045678704, 7.238244758476583, 6.8273566609015575, 6.5757213798790985, 6.49098472608938, 6.49098472608938, 7.236196056970243, 7.964760598143869, 8.660403421677188, 9.307585016407472, 9.89184841494398, 10.400142138429544, 10.821111745011592, 11.145353469310407, 11.365624286998468, 11.47700371199528, 11.47700371199528, 12.352646370383525, 13.209626225755414, 14.029678240339479, 14.795324429178322, 15.490246373033699, 16.099633017161125, 16.610496343227794, 17.011948186374184, 17.29543229754421, 17.45490670507934, 17.45490670507934, 17.470560371735132, 17.477400983250334, 17.483113637730128, 17.4874118788249, 17.490080174778313, 17.49098472608938, 17.49098472608938, 16.779410534722956, 16.082321941882242, 15.41390966107997, 14.787780638811398, 14.216681056362972, 13.712236854318064, 13.284717061933463, 12.942824749316799, 12.693519858016884, 12.541877516684735, 12.49098472608938, 12.49098472608938, 12.450068636297544, 12.328436451191294, 12.12940597970885, 11.858406254278577, 11.522829440966575, 11.13182920045665, 10.696071000048278, 10.227441187511772, 10.227441187511772, 10.988726282598975, 11.725543048342615, 12.414209587636833, 13.032591591272023, 13.560813752844865, 13.981898577459901, 14.2823120523841, 14.452398641352488, 14.452398641352488, 15.399776833135718, 15.804231264915927, 16.13012209832624, 16.35850974269397, 16.476121138283588, 16.476121138283588, 15.436813107218086, 14.429083924761244, 13.483552932186512, 12.62894961024142, 11.891240648413145, 11.292840960208139, 10.851932617347643, 10.581912396719076, 10.49098472608938};
        double[] yDABrush = new double[]{49.44979587787088, 47.146230591109884, 44.85700445653032, 42.58647514619037, 40.338964741680684, 38.11875150679646, 35.93006174361898, 33.777061747509435, 31.663849876328868, 29.594448748981222, 27.57279758812956, 25.60274472166151, 23.68804025717907, 21.83232894345508, 20.039143232446747, 18.31189655507319, 16.653876823554924, 15.068240172686728, 13.55800495195507, 12.126045979938908, 10.775089071928562, 9.507705851181925, 8.326308853692979, 7.233146935792718, 6.2303009933230555, 5.319680000533481, 4.503017376240223, 3.781867684164638, 3.1576036737329787, 2.6314136669700474, 2.2042992964603627, 1.8770735986837508, 1.6503594663543595, 1.5245884627084934, 1.5, 1.576640883765208, 1.7543652237259266, 1.7543652237259266, 2.675594546562195, 3.2611654115885926, 3.928626549649323, 4.676724872199202, 5.504055902805078, 6.40906641390302, 7.390057342821137, 8.445186981592144, 9.572474434567596, 10.76980333734241, 12.034925830008206, 13.36546677727489, 14.758928227539627, 16.21269410252944, 17.724035108714986, 19.29011386127422, 20.90799021098536, 22.574626764050663, 24.286894584485026, 26.041579068366957, 27.83538597892141, 29.664947631105008, 31.52682921408291, 33.41753523972733, 35.333516105031094, 37.271174756116466, 39.22687344132811, 41.19694054073136, 43.177677459195536, 45.16536557011926, 47.15627319676372, 49.14666261808583, 51.132797085918696, 53.11094784032616, 55.07740110995974, 55.07740110995974, 53.77625867521658, 52.380227671260855, 51.20912197710152, 50.5312277939168, 50.5018425095638, 51.127697938556764, 52.26541814448905, 53.654365139196216, 54.97634790481669, 55.928516121366386, 56.292739525304626, 55.985578924156925, 55.985578924156925, 57.03542184802592, 58.31610869635574, 60.05106387993601, 62.184524511640575, 64.64791941134916, 67.36207304009713, 70.23975026704346, 70.23975026704346, 67.92609166420385, 65.66940301475881, 63.52525148277789, 61.54643319500951, 59.7816732250198, 58.274425820172496, 57.06180441384231, 56.17366776951275, 55.63188475892383, 55.44979587787088, 55.44979587787088, 56.41471394529634, 57.593389417319884, 59.19308478796722, 61.16519412288625, 63.44979587787088, 65.97747358466017, 68.6714250352, 71.44979587787088, 71.44979587787088, 68.81628243337911, 66.25460437059593, 63.83463758927769, 61.622392474836204, 59.678213307100805, 58.055132225670434, 56.79742265138998, 55.93939162284158, 55.93939162284158, 57.27734225131309, 58.886936490418975, 61.02619660587129, 63.587851162709, 66.44344810491441, 69.44979587787088, 69.44979587787088, 68.23943228486729, 66.77603379912011, 64.8176243668949, 62.44979587787088, 59.776033799120114, 56.913194363618004, 56.913194363618004, 57.03598560512489, 57.400826849192924, 57.99722227749493, 58.80801467941319, 59.80987903375052, 60.97399352712239, 62.2668687050525, 63.651310902603655, 65.08749223839993, 65.08749223839993, 63.26695543393993, 61.49124628762098, 59.804088651866266, 58.24702594702319, 56.858398224479856, 55.67239810735646, 54.718228854645304, 54.019385280054394, 53.5930752317189, 53.44979587787088, 53.44979587787088, 53.60378837314232, 54.06313100466764, 54.81996429266775, 55.86133860975099, 57.16943575262866, 58.72187381651304, 60.492090155713925, 62.44979587787088, 64.56149409529928, 66.79105306602548, 69.10032441790992, 69.10032441790992, 67.07723136807556, 65.11560897682597, 63.27506014789043, 61.611509026073406, 60.17550177041397, 59.01067073472052, 58.152408720579615, 57.62679358498946, 57.44979587787088, 57.44979587787088, 57.699587232628915, 58.43936195522417, 59.640690917937775, 61.25740772244575, 63.22738284861606, 65.47491125712475, 67.9136216916612, 70.44979587787088, 70.44979587787088, 69.48487781044543, 68.30620233842188, 66.70650696777454, 64.73439763285552, 62.44979587787088, 59.92211817108159, 57.22816672054182, 54.44979587787094, 54.44979587787094, 54.59898872481938, 55.04338747915324, 55.773520552966886, 56.773826427412644, 58.02298531945797, 59.49437357604643, 61.15663111103987, 62.9743297910087, 64.90872852438736, 66.91859896058065, 66.91859896058065, 65.31836347213664, 63.764163845153746, 62.30071157317616, 60.97010751356777, 59.810630722498786, 58.8556372361943, 58.13260047850292, 57.66232090051369, 57.458327589449596, 57.458327589449596, 57.65569718442134, 58.24180899406724, 59.1988543076381, 60.49775378756931, 62.09904103043806, 63.95406173366024, 66.00645203185417, 68.19385108436882, 70.44979587787088, 70.44979587787088, 70.27108909747295, 69.73896077044913, 68.86529776430962, 67.66961626692682, 66.1786258271481, 64.42563270761065, 62.44979587787094, 60.29525226773325, 58.01013082117197, 55.64547737525373, 55.64547737525373, 55.81389818035984, 56.31557099862118, 57.13980354489587, 58.269028733451535, 59.67917908989875, 61.34019970762267, 63.2166888159378, 65.2686523073674, 67.4523561426085, 69.72125846565086, 69.72125846565086, 68.30793936889472, 66.60767462986195, 64.34790725896016, 61.64195142813321, 58.625495061826314, 55.44979587787088, 55.44979587787088, 55.622831365895024, 56.13841532642442, 56.98605195684405, 58.14848581974081, 59.60205311384851, 61.31716340080101, 63.25890198112569, 65.38774065683879, 67.66034241156655, 70.030443627225, 72.44979587787088, 72.44979587787088, 69.81628243337911, 67.25460437059593, 64.83463758927769, 62.622392474836204, 60.678213307100805, 59.055132225670434, 57.79742265138998, 56.93939162284158, 56.93939162284158, 57.15650937741873, 57.80088429787298, 58.85180564406744, 60.27549593923453, 62.02619660587129, 64.04763868407065, 66.27485136219121, 68.63625019220154, 68.63625019220154, 67.35490925618933, 65.82297716607235, 63.80442621476078, 61.41656724641058, 58.79817394474816, 58.79817394474816, 58.99038050995233, 59.5611601063423, 60.4931698847102, 61.758091199560056, 63.3174900568211, 65.12398491130949, 67.12268633100206, 69.25286478465313, 71.44979587787088};

        //Transparent and mouse transparent rectangle surrounding group at base:
        Rectangle backgroundMaskGr = new Rectangle(0.0,0.0,110.62875366210938, 140.45559692382812);
        backgroundMaskGr.setFill(Color.TRANSPARENT);
        backgroundMaskGr.setStroke(Color.TRANSPARENT);
        backgroundMaskGr.setMouseTransparent(true);
        maskGr.getChildren().add(backgroundMaskGr);

        Canvas maskGrCanv = new Canvas(101.85734329855813, 41.0);
        GraphicsContext grConMaskGrCanv = maskGrCanv.getGraphicsContext2D();
        grConMaskGrCanv.setFill(Color.valueOf("0x000000ff"));
        grConMaskGrCanv.setStroke(Color.valueOf("0x000000ff"));
        grConMaskGrCanv.setLineWidth(1.0);
        grConMaskGrCanv.setLineJoin(StrokeLineJoin.ROUND);
        grConMaskGrCanv.fillPolygon(xDAMaskGrCanv, yDAMaskGrCanv, 83);
        grConMaskGrCanv.strokePolygon(xDAMaskGrCanv, yDAMaskGrCanv, 83);
        maskGrCanv.setTranslateX(7.628753662109375);
        maskGrCanv.setTranslateY(99.45559692382812);
        Canvas maskGrCanvB = new Canvas(56.15869053041189, 94.37180348806226);
        GraphicsContext grConMaskGrCanvB = maskGrCanvB.getGraphicsContext2D();
        grConMaskGrCanvB.setFill(Color.valueOf("0x000000ff"));
        grConMaskGrCanvB.setStroke(Color.valueOf("0x000000ff"));
        grConMaskGrCanvB.setLineWidth(1.0);
        grConMaskGrCanvB.setLineJoin(StrokeLineJoin.ROUND);
        grConMaskGrCanvB.fillPolygon(xDAMaskGrCanvB, yDAMaskGrCanvB, 78);
        grConMaskGrCanvB.strokePolygon(xDAMaskGrCanvB, yDAMaskGrCanvB, 78);
        maskGrCanvB.setTranslateX(-5.026819962949958E-6);
        maskGrCanvB.setTranslateY(0.08379343576586962);
        Canvas maskGrCanvC = new Canvas(65.80654685481926, 96.45030618971617);
        GraphicsContext grConMaskGrCanvC = maskGrCanvC.getGraphicsContext2D();
        grConMaskGrCanvC.setFill(Color.valueOf("0x999999ff"));
        grConMaskGrCanvC.setStroke(Color.valueOf("0x000000ff"));
        grConMaskGrCanvC.setLineWidth(1.0);
        grConMaskGrCanvC.setLineJoin(StrokeLineJoin.ROUND);
        grConMaskGrCanvC.fillPolygon(xDAMaskGrCanvC, yDAMaskGrCanvC, 144);
        grConMaskGrCanvC.strokePolygon(xDAMaskGrCanvC, yDAMaskGrCanvC, 144);
        maskGrCanvC.setTranslateX(4.0159124961782595);
        maskGrCanvC.setTranslateY(8.239057592618337E-6);
        Canvas maskGrCanvD = new Canvas(103.0, 48.98935109107492);
        GraphicsContext grConMaskGrCanvD = maskGrCanvD.getGraphicsContext2D();
        grConMaskGrCanvD.setFill(Color.valueOf("0xffcce6ff"));
        grConMaskGrCanvD.setStroke(Color.valueOf("0x000000ff"));
        grConMaskGrCanvD.setLineWidth(1.0);
        grConMaskGrCanvD.setLineJoin(StrokeLineJoin.ROUND);
        grConMaskGrCanvD.fillPolygon(xDAMaskGrCanvD, yDAMaskGrCanvD, 146);
        grConMaskGrCanvD.strokePolygon(xDAMaskGrCanvD, yDAMaskGrCanvD, 146);
        maskGrCanvD.setTranslateX(7.628753662109375);
        maskGrCanvD.setTranslateY(78.46092137829066);
        GraphicsContext grConBrush = brush.getGraphicsContext2D();
        grConBrush.setFill(Color.valueOf("0xcc9933ff"));
        grConBrush.setStroke(Color.valueOf("0x000000ff"));
        grConBrush.setLineWidth(1.0);
        grConBrush.setLineJoin(StrokeLineJoin.ROUND);
        grConBrush.fillPolygon(xDABrush, yDABrush, 304);
        grConBrush.strokePolygon(xDABrush, yDABrush, 304);

        maskShadow = new DropShadow();
        maskShadow.setOffsetX(7.0);
        maskShadow.setOffsetY(3.0);
                
        maskGr.setScaleX(0.25);
        maskGr.setScaleY(0.25);
        
        brush.setScaleX(0.25);
        brush.setScaleY(0.25);
        
        maskGr.getChildren().addAll(maskGrCanv, maskGrCanvB, maskGrCanvC, maskGrCanvD);
        maskGr.setTranslateX(1011.1856231689453);
        maskGr.setTranslateY(55.27220153808594);
        brush.setTranslateX(1011.1856231689453 + 39.637768936019995);
        brush.setTranslateY(55.27220153808594 + 33.50580104595724);
        
        mask.getChildren().addAll(maskGr, brush);
        mask.setEffect(maskShadow);
        
        //Functionality to discover the desired translated position:
        maskGr.setOnMouseEntered((mE)->{
            mask.setEffect(null);
            mask.setTranslateX(mask.getTranslateX() + 7.0);
            mask.setTranslateY(mask.getTranslateY() + 3.0);
        });
        maskGr.setOnMouseExited((mE)->{
            mask.setTranslateX(mask.getTranslateX() - 7.0);
            mask.setTranslateY(mask.getTranslateY() - 3.0);            
            mask.setEffect(maskShadow);
        });
        maskGr.setOnMousePressed((mP)->{
            if(brush.isMouseTransparent()){
                resetMasker();                
            } else {
                brush.setMouseTransparent(true);
                sXBrush = brush.getTranslateX();
                sYBrush = brush.getTranslateY();
            }
        });

        //End of imported canvii and functions.
        
    }
    
    private void resetMasker(){
        brush.setMouseTransparent(false);
        brush.setTranslateX(sXBrush);
        brush.setTranslateY(sYBrush);
    }
    
    private void completeSelectionTransformation(){
        transformationAreaGon.getPoints().clear();
        if(transformationArea.getPoints().size() > 3){
            for(double p : transformationArea.getPoints()){
                transformationAreaGon.getPoints().add(p);
            }        
            transformationAreaGon.setTranslateX(0.0);
            transformationAreaGon.setTranslateY(0.0);
            transformationAreaGon.setVisible(true);        
            resizeArrow(transformationAreaGon.getBoundsInParent());
            resizeArrow.setVisible(true);
        }        
        transformationArea.getPoints().clear();
        transformationArea.setVisible(false);        
    }
    
    private void resetTransformationSelection(){        
        transformationAreaGon.getPoints().clear();
        transformationArea.getPoints().clear();
        transformationAreaGon.setVisible(false);
        transformationArea.setVisible(false);
        transformationAreaGon.setTranslateX(0.0);
        transformationAreaGon.setTranslateY(0.0);
        transformationArea.setTranslateX(0.0);
        transformationArea.setTranslateY(0.0);
        transformationAreaGon.setScaleX(1.0);
        transformationAreaGon.setScaleY(1.0);
        resizeArrow.setVisible(false);
        resizeArrow.setScaleX(1.0);
        resizeArrow.setScaleY(1.0);
        
        cutImageView.setImage(null);
        cutImageView.setVisible(false);
        transfSelReader = null;
        transfSelWriter = null;   
    }
    
    private void paperizeSelection(){
        Color paperColor;
        
        paperColor = Color.TRANSPARENT;
        if((pWriter != null) && (transformationAreaGon.getPoints().size() > 0)){
            
            if(!actionsPerformed.get(actionsPerformed.size() -1).equals(Actions.SCALE_REF_AREA)){
                generateSPGraphicsRecovery();     
            }
            Bounds bsTransfSel = transformationAreaGon.getBoundsInParent();
            for(int yIndT = (int) bsTransfSel.getMinY(); yIndT <= (int) bsTransfSel.getMaxY(); yIndT++){
                for(int xIndT = (int) bsTransfSel.getMinX(); xIndT <= (int) bsTransfSel.getMaxX(); xIndT++){
                    if(transformationAreaGon.contains(xIndT,yIndT)){
                        if((xIndT < (int) shrunkPiece.getWidth()) &&
                                (yIndT < (int) shrunkPiece.getHeight())){
                            pWriter.setColor(xIndT, yIndT, paperColor);
                        }
                    }
                }
            }
            storeShrunkPieceGraphics();
        }
        
    }
    private void transferImage(){
        if((transformationAreaGon.getPoints().size() > 5) &&
                (pReader != null) && (pWriter != null)) {
            int minX, minY, widthTransfSel, heightTransfSel;
            Color paperColor;
            
            if(!actionsPerformed.get(actionsPerformed.size()-1).equals(Actions.SCALE_REF_AREA)){
                generateSPGraphicsRecovery();
            }
            paperColor = Color.TRANSPARENT;
            Bounds bsTransfSel = transformationAreaGon.getBoundsInParent();
            cutOutImage = new WritableImage((int)sPContImage.getWidth(), (int)sPContImage.getHeight());
            transfSelReader = cutOutImage.getPixelReader();
            transfSelWriter = cutOutImage.getPixelWriter(); 
            
            minX = (int)bsTransfSel.getMinX();
            minY = (int)bsTransfSel.getMinY();
            if(minX < 0){ minX = 0;}
            if(minY < 0){ minY = 0;}
            widthTransfSel = ((int)bsTransfSel.getMaxX()-minX);
            heightTransfSel = ((int)bsTransfSel.getMaxY()-minY);
            if((pReader != null) && (transfSelWriter != null)){
                transfSelWriter.setPixels(minX,minY,widthTransfSel,heightTransfSel,
                                    pReader,minX,minY);                
                for(int yIndT = minY; yIndT < (int) bsTransfSel.getMaxY(); yIndT++){
                    for(int xIndT = minX; xIndT < (int) bsTransfSel.getMaxX(); xIndT++){
                        if(!transformationAreaGon.contains(xIndT,yIndT)){ 
                            transfSelWriter.setColor(xIndT, yIndT, Color.TRANSPARENT);                            
                        } else {
                            if((xIndT < (int) shrunkPiece.getWidth()) &&
                                (yIndT < (int) shrunkPiece.getHeight())){
                                pWriter.setColor(xIndT, yIndT, paperColor);
                            }
                        }
                    }
                }               
            }
            storeShrunkPieceGraphics();
            
            cutImageView.setScaleX(1.0);
            cutImageView.setScaleY(1.0);
            cutImageView.setImage(cutOutImage);
            Rectangle2D tSelR = new Rectangle2D(minX, minY, widthTransfSel,heightTransfSel);
            cutImageView.setViewport(tSelR);
            cutImageView.setTranslateX(minX);
            cutImageView.setTranslateY(minY);
            cutImageView.setVisible(true);
            if(sPieceSPaneContentWShade.getChildren().indexOf(cutImageView) < 0){
                if(sPieceSPaneContentWShade.getChildren().indexOf(transformationArea) > 0){
                    sPieceSPaneContentWShade.getChildren().add(
                            sPieceSPaneContentWShade.getChildren().indexOf(transformationArea),cutImageView);
                } else {
                    sPieceSPaneContentWShade.getChildren().add(cutImageView);
                }
            }
        }
        
    }
    
    private void settlePieceExcerpt(){
        if((transfSelReader != null) && (pWriter != null)){
            double xOffGon, yOffGon;
            Color tSColor;   
                         
            Bounds bsTransfSel = transformationAreaGon.getBoundsInParent();
            xOffGon = transformationAreaGon.getTranslateX();
            yOffGon = transformationAreaGon.getTranslateY();
            
            for(int yIndT = (int) bsTransfSel.getMinY(); yIndT < (int) bsTransfSel.getMaxY(); yIndT++){
                for(int xIndT = (int) bsTransfSel.getMinX(); xIndT < (int) bsTransfSel.getMaxX(); xIndT++){                    
                    if(transformationAreaGon.contains(xIndT - xOffGon,yIndT - yOffGon)){                        
                        tSColor = transfSelReader.getColor(((int) (xIndT - xOffGon)),((int)(yIndT - yOffGon)));                         
                        if((xIndT < (int) shrunkPiece.getWidth()) &&
                            (yIndT < (int) shrunkPiece.getHeight())){
                            pWriter.setColor(xIndT, yIndT, tSColor);//tSColor);                             
                        }                        
                    }   
                }
            }

            storeShrunkPieceGraphics();
        }
        
    }
    
    private void scaleTransformationSelection(double scaleX, double scaleY){
        if((transfSelReader != null) && (pWriter != null)){
            int xOrig, yOrig, xCent, yCent, xOffset, yOffset, xCont, yCont, sWidth, sHeight;
            Color tSColor;             
            
            Bounds tABoundsPreScale = preScaleTransfAGon.getBoundsInParent();
            sWidth = (int) (tABoundsPreScale.getWidth()*scaleX);
            sHeight = (int) (tABoundsPreScale.getHeight()*scaleY);
            xCent = (int)(tABoundsPreScale.getMinX() + (tABoundsPreScale.getWidth()*0.5));
            xOffset = (int)(xCent - (((double)sWidth)*0.5));
            yCent = (int)(tABoundsPreScale.getMinY() + (tABoundsPreScale.getHeight()*0.5));
            yOffset =  (int)(yCent - (((double)sHeight)*0.5));
            for(int yIndT = yOffset; yIndT < (int) (yOffset + sHeight); yIndT++){
                yOrig = yCent + ((int) (((double) (yIndT-yCent))/scaleY)); 
                for(int xIndT = xOffset; xIndT < (int) (xOffset + sWidth); xIndT++){
                    xOrig = xCent + ((int) (((double) (xIndT-xCent))/scaleX));
                    if(preScaleTransfAGon.contains(xOrig, yOrig)){
                        tSColor = transfSelReader.getColor(xOrig,yOrig);
                        //xCont = xCent + xIndT;
                        //yCont = yCent + yIndT;
                        if((xIndT < (int) shrunkPiece.getWidth()) &&
                                (yIndT < (int) shrunkPiece.getHeight())){
                            pWriter.setColor(xIndT, yIndT, tSColor);
                        }
                    }
                }
            }
            storeShrunkPieceGraphics();
        }
        
    }
    
    private void initResizingArrow(){
        arrColor = Color.ORANGE;
        
        arrShunt = new Line();
        arrShunt.setStroke(arrColor);
        leftPoint = new Line();
        leftPoint.setStroke(arrColor);
        rightPoint = new Line();
        rightPoint.setStroke(arrColor);
        rDot = new Circle(dotRad);
        rDot.setFill(arrColor);
        rDot.setStroke(Color.TRANSPARENT);
        rDot.setVisible(false);
        arrShunt.setStrokeWidth(arrWidth);
        leftPoint.setStrokeWidth(arrWidth);
        rightPoint.setStrokeWidth(arrWidth);
        
        resizeArrow.getChildren().addAll(arrShunt, leftPoint, rightPoint, rDot);    
        resizeArrow.setVisible(false);
        
        resizeArrow.setOnMouseEntered((mE)->{
            arrShunt.setStrokeWidth(arrWidth + 2.0);
            leftPoint.setStrokeWidth(arrWidth + 2.0);
            rightPoint.setStrokeWidth(arrWidth + 2.0);  
            rDot.setVisible(true);
        });
        resizeArrow.setOnMouseExited((mEx)->{
            arrShunt.setStrokeWidth(arrWidth);
            leftPoint.setStrokeWidth(arrWidth);
            rightPoint.setStrokeWidth(arrWidth);
            rDot.setVisible(false);            
        });
        resizeArrow.setOnMousePressed((mP)->{
            sXMA = mP.getSceneX();
            sYMA = mP.getSceneY();
            sXLA = Math.abs(arrShunt.getEndX() - arrShunt.getStartX());
            sYLA = Math.abs(arrShunt.getEndY() - arrShunt.getStartY());
            transferImage();                
            actionsPerformed.add(Actions.SCALE_REF_AREA);            
            preScaleTransfAGon.getPoints().clear();
            for(double p : transformationAreaGon.getPoints()){
                preScaleTransfAGon.getPoints().add(p);
            }
        });
        resizeArrow.setOnMouseDragged((mD)->{
            double dist, distX, distY, scXIV, scYIV, scXTA, scYTA; 
            if(transformationAreaGon.getPoints().size() > 0){
                scXIV = cutImageView.getScaleX();
                scYIV = cutImageView.getScaleY();
                scXTA = transformationAreaGon.getScaleX();
                scYTA = transformationAreaGon.getScaleY();
                if(!shiftPiecePane){
                    dist = Math.abs(mD.getSceneX() - sXMA + sXLA);                    
                    cutImageView.setScaleX(dist/sXLA);
                    transformationAreaGon.setScaleX(dist/sXLA);
                    dist = Math.abs(mD.getSceneY() - sYMA + sYLA); 
                    cutImageView.setScaleY(dist/sYLA);
                    transformationAreaGon.setScaleY(dist/sYLA);
                } else {                    
                    distX = Math.abs(mD.getSceneX() - sXMA + sXLA);
                    distY = Math.abs(mD.getSceneY() - sYMA + sYLA);
                    if(distX > distY){
                        dist = distX;
                    } else {
                        dist = distY;
                    }
                    cutImageView.setScaleX(dist/sXLA);
                    transformationAreaGon.setScaleX(dist/sXLA);                    
                    cutImageView.setScaleY(dist/sYLA);
                    transformationAreaGon.setScaleY(dist/sYLA);
                }
                if((transformationAreaGon.getBoundsInParent().getMinX() >= 0.0) &&
                        (transformationAreaGon.getBoundsInParent().getMinY() >= 0.0)){
                    resizeArrow(transformationAreaGon.getBoundsInParent());
                } else {
                    cutImageView.setScaleX(scXIV);
                    transformationAreaGon.setScaleX(scXTA);                    
                    cutImageView.setScaleY(scYIV);
                    transformationAreaGon.setScaleY(scYTA);
                }
            }
        });
        resizeArrow.setOnMouseReleased((mR)->{  
            double scX, scY, tXSc, tYSc, xMin, xMax, yMin, yMax, xCent, yCent;
            ArrayList<Double> pScaledTransSel;
            scX = transformationAreaGon.getScaleX();
            scY = transformationAreaGon.getScaleY();
            if((scX != 1.0) || (scY != 1.0)){
                scaleTransformationSelection(scX, scY);
            }
            
            //Keep selection available for transformation:
            pScaledTransSel = new ArrayList<Double>();
            xMin = transformationAreaGon.getPoints().get(0);
            xMax = xMin;
            yMin = transformationAreaGon.getPoints().get(1);
            yMax = yMin;
            for(int pInd = 0; pInd < transformationAreaGon.getPoints().size(); pInd += 2){
                if(transformationAreaGon.getPoints().get(pInd) < xMin){
                    xMin = transformationAreaGon.getPoints().get(pInd);
                } else if(transformationAreaGon.getPoints().get(pInd) > xMax){
                    xMax = transformationAreaGon.getPoints().get(pInd);
                }
                if(transformationAreaGon.getPoints().get(pInd+1) < yMin){
                    yMin = transformationAreaGon.getPoints().get(pInd+1);
                } else if(transformationAreaGon.getPoints().get(pInd+1) > yMax){
                    yMax = transformationAreaGon.getPoints().get(pInd + 1);
                }
            }
            xCent = xMin + (xMax - xMin)*0.5;
            yCent = yMin + (yMax - yMin)*0.5;
            for(int pInd = 0; pInd < transformationAreaGon.getPoints().size(); pInd += 2){
                pScaledTransSel.add(((transformationAreaGon.getPoints().get(pInd) - xCent)*scX) + xCent);
                pScaledTransSel.add(((transformationAreaGon.getPoints().get(pInd + 1) - yCent)*scY) + yCent);
            }
            tXSc = transformationAreaGon.getTranslateX();
            tYSc = transformationAreaGon.getTranslateY();
            resetTransformationSelection();
            transformationAreaGon.getPoints().addAll(pScaledTransSel);
            transformationAreaGon.setVisible(true);
            resizeArrow.setVisible(false);
        });
    }
    
    public void resizeArrow(Bounds to){  
        double lShunt, aAngle, pAngle;
       
        arrShunt.setStartX(to.getMinX() + (to.getWidth()*0.5));
        arrShunt.setStartY(to.getMinY() + (to.getHeight()*0.5));
        arrShunt.setEndX(to.getMaxX());
        arrShunt.setEndY(to.getMaxY());
        
        aAngle = Math.asin(to.getHeight()/(Math.sqrt(Math.pow(to.getHeight(),2.0) +
                Math.pow(to.getWidth(),2.0))));        
        leftPoint.setStartX(to.getMaxX());
        leftPoint.setStartY(to.getMaxY());
        pAngle = (0.75*Math.PI) - aAngle;
        leftPoint.setEndX(to.getMaxX() + (Math.cos(pAngle)*10.0));
        leftPoint.setEndY(to.getMaxY() - (Math.sin(pAngle)*10.0));
        rightPoint.setStartX(to.getMaxX());
        rightPoint.setStartY(to.getMaxY());        
        pAngle = (1.25*Math.PI) - aAngle;
        rightPoint.setEndX(to.getMaxX() + (Math.cos(pAngle)*10.0));
        rightPoint.setEndY(to.getMaxY() - (Math.sin(pAngle)*10.0));
        rDot.setTranslateX(to.getMaxX());
        rDot.setTranslateY(to.getMaxY());
    }
    
    private void deleteMarkedArea(){
            paperizeSelection();
            resetTransformationSelection();
            setClearAreas();
    }    
    
    private void updateTXShrunkPieces(){
        shrunkPieces.setTranslateX((sPiecePane.getBoundsInParent().getMinX()-ShrunkPiece.WSuP) - 3.0);
    }
    
    private void storeSPContImage(){
        if(sPContImage != null){
            ShrunkPiece newShrunk = new ShrunkPiece(sPContImage);
            shrunkPieces.getChildren().add(newShrunk);
            newShrunk.setOnMousePressed((mP)->{                
                if(sPCont != null){
                    double sPWidth, sPHeight;
                    
                    generateSPGraphicsRecovery();
                    
                    sPWidth = shrunkPiece.getWidth();
                    sPHeight = shrunkPiece.getHeight() + 
                            newShrunk.getHeightOfTheShrunkPiece();
                    
                    if(newShrunk.getWidthOfTheShrunkPiece() > sPWidth){
                        sPWidth = newShrunk.getWidthOfTheShrunkPiece();
                    }
                    
                    sPContImage = new WritableImage((int)shrunkPiece.getWidth(),
                        (int)shrunkPiece.getHeight());
                    shrunkPiece.snapshot(null, sPContImage); 
                    
                    clearShrunkPieceContext();
                    shrunkPiece.setWidth(sPWidth);
                    shrunkPiece.setHeight(sPHeight);
                    sPCont.drawImage(newShrunk.getShrunkPiece(), 0.0, 0.0);
                    sPCont.drawImage(sPContImage, 0.0, newShrunk.getHeightOfTheShrunkPiece());
                    
                    storeShrunkPieceGraphics();
                    
                    shrunkPiecePrepended = newShrunk;
                    indPrepShrunkPiece = shrunkPieces.getChildren().indexOf(newShrunk);
                    shrunkPieces.getChildren().remove(newShrunk);
                    
                    actionsPerformed.add(Actions.SHRUNK_PIECE_PREPEND);
                }
            });
        }
    }
    
    private void undoShrunkPiecePrependance(){
        recoverShrunkPieceGraphics();
        if(shrunkPiecePrepended != null){
            if((indPrepShrunkPiece >= 0) && 
                    (indPrepShrunkPiece < shrunkPieces.getChildren().size())){
                shrunkPieces.getChildren().add(indPrepShrunkPiece, shrunkPiecePrepended);
            } else {
                shrunkPieces.getChildren().add(shrunkPiecePrepended);
            }
        }        
    }
    
    private void orderStroredShrunk(){
        double sumHeight;
        
        sumHeight = 0.0;
        for(Node sN : shrunkPieces.getChildren()){
            sN.setTranslateX(0.0);
            sN.setTranslateY(sumHeight);
            sumHeight += ShrunkPiece.HSuP + 20.0;
        }
        
    }
    
    private void initTextButton(){
        tFLabel.setText("Insert...");
        tFLabel.setEditable(false);
        tFLabel.setPrefWidth(100.0);
        tFLabel.setPrefHeight(25.0);
        tFLabel.setFocusTraversable(false);
        tFLabel.selectionProperty().addListener(new ChangeListener<IndexRange>(){
            @Override
            public void changed(ObservableValue<? extends IndexRange> observable, 
                    IndexRange oldValue, IndexRange newValue) {
                    if(newValue.getLength() > 1){
                        tFLabel.deselect();
                    }
            }
        });
        
        tFLabel.setTranslateX(sPiecePane.getBoundsInParent().getMinX() + 
                boundsMBounds.getMaxX() - tFLabel.getPrefWidth() - 88.0);
        tFLabel.setTranslateY(sPiecePane.getTranslateY() - 
                tFLabel.getPrefHeight() - 12.0);

        xOffTBShadow = 7.0; yOffTBShadow = 3.0;
        textButtShadow = new DropShadow();
        textButtShadow.setOffsetX(xOffTBShadow);
        textButtShadow.setOffsetY(yOffTBShadow);
        tFLabel.setEffect(textButtShadow);

        xInPPane = 0.0; yInPPane = 0.0;
        insertionLine = new Line(0.0,0.0,0.0,-25.0);
        insertionLine.setStroke(Color.PURPLE);
        insertionLine.setVisible(false);
        selFontFamily = "Times New Roman";
        textAddField = new TextField();
        textAddField.setPrefWidth(200.0);
        textAddField.setTranslateY(insertionLine.getEndY() - 25.0);
        textAddField.textProperty().addListener(new ChangeListener<String>(){
            @Override
            public void changed(ObservableValue<? extends String> observable, 
                    String oldValue, String newValue) {                 
                if(tPreviewOffset == null){
                    tPreviewOffset = addTextToSPContext(newValue);                    
                } else {
                    if(sPCont != null){
                        sPCont.clearRect(tPreviewOffset[0], tPreviewOffset[1] - 20.0,
                                ((tPreviewOffset[2]/2.2)*textEntered.length()), 
                                (6 + (tPreviewOffset[2]/2.0)) + 20.0);
                    }
                    tPreviewOffset = addTextToSPContext(newValue);
                }
                textEntered = newValue;
            }
        });
        textAddField.setOnKeyPressed((kP)->{
            if(kP.getCode().equals(KeyCode.ESCAPE)){
                if(sPContImage != null){
                    clearShrunkPieceContext();
                    sPCont.drawImage(sPContImage, 0.0,0.0);
                }     
                hideTextEntry();
            }
        });
        textAddField.setOnAction((ac)->{
            tPreviewOffset = null;
            textEntered = "";
            if(sPCont != null){         
                if(sPContImage != null){
                    clearShrunkPieceContext();
                    sPCont.drawImage(sPContImage, 0.0,0.0);
                }
                
                addTextToSPContext(textAddField.getText());

                storeShrunkPieceGraphics();

                hideTextEntry();                
            }
        });
        fontSize = new TextField("12.2");
        fontSize.setOnAction((ac)->{
            textAddField.requestFocus();
        });
        fontSize.setPrefWidth(50.0);
        fontSize.setTranslateY(textAddField.getTranslateY());
        fontSize.setTranslateX(textAddField.getPrefWidth() + 5.0);    
        textEntry.setVisible(false);
        textEntry.getChildren().addAll(insertionLine, fontSize, textAddField);

        tFLabel.setOnMouseEntered((mEn)->{ 
            textButtShadow.setOffsetX(xOffTBShadow - 3.0);
            textButtShadow.setOffsetY(yOffTBShadow - 2.0);
        });
        tFLabel.setOnMouseExited((mEx)->{
            textButtShadow.setOffsetX(xOffTBShadow);
            textButtShadow.setOffsetY(yOffTBShadow);
        });
        tFLabel.setOnMousePressed((mP)->{ 
            tFLabel.setTranslateX(tFLabel.getTranslateX() + xOffTBShadow);
            tFLabel.setTranslateY(tFLabel.getTranslateY() + yOffTBShadow);
            tFLabel.setEffect(null);
            showTextEntry(mP.getSceneX(), mP.getSceneY());
        });
        tFLabel.setOnMouseReleased((mR)->{
            tFLabel.setTranslateX(tFLabel.getTranslateX() - xOffTBShadow);
            tFLabel.setTranslateY(tFLabel.getTranslateY() - yOffTBShadow);
            tFLabel.setEffect(textButtShadow);
        });

        //End of imported canvii and functions.        
        
    }
    
    private double[] addTextToSPContext(String newValue){                
                double  sHeight, sWidth;
                double[] tPoint;
                
                tPoint = new double[3];
                if(xInPPane > shrunkPiece.getWidth()){
                    shrunkPiece.setWidth(sPiecePane.getBoundsInLocal().getWidth());
                }
                if(yInPPane > shrunkPiece.getHeight()){
                    shrunkPiece.setHeight(sPiecePane.getBoundsInLocal().getHeight());
                }
                sWidth = (shrunkPiece.getWidth() - sPiecePane.getBoundsInLocal().getWidth());
                if(sWidth < 0.0) { sWidth = 0.0;}
                tPoint[0] = (sPiecePane.getHvalue()*sWidth) + xInPPane - 25.0;
                sHeight = (shrunkPiece.getHeight() - sPiecePane.getBoundsInLocal().getHeight());
                if(sHeight < 0.0) { sHeight = 0.0;}
                tPoint[1] = (sPiecePane.getVvalue()*sHeight) + yInPPane - 25.0;
                if(tPoint[0] < 0.0) { tPoint[0] = 0.0;}
                if(tPoint[1] < 0.0) { tPoint[1] = 0.0;}
                
                sPCont.setFont(getEntryFont());
                tPoint[2] = getEntryFontSize();
                sPCont.fillText(newValue, tPoint[0], tPoint[1]);
                
                actionsPerformed.add(Actions.ENTER_TEXT);
                return tPoint;
    }
    
    private void showTextEntry(double atX, double atY){
        storeShrunkPieceGraphics();        
        generateSPGraphicsRecovery();        
        ornamentation = true;
        textEntry.setTranslateX(atX);
        textEntry.setTranslateY(atY);
        textAddField.selectAll();
        textEntry.setVisible(true);
        sPiecePane.requestFocus();
    }
    
    private void hideTextEntry(){
        insertionLine.setVisible(false);
        textEntry.setVisible(false);
        ornamentation = false;  
        sPiecePane.requestFocus();
    }
    
    private Font getEntryFont(){        
        return Font.font(selFontFamily, FontWeight.THIN, getEntryFontSize());        
    }
    
    private double getEntryFontSize(){
        double fS;
        
        fS = Double.valueOf(fontSize.getText());
        if(fS == Double.NaN){
            fS = 12.0;
        } else if(fS < 0.0){
            fS = 1.0;
        }
        fontSize.setText(String.valueOf(fS));        
        return fS;
    }
    
    private WritableImage transperizeImage(Image aTransparent, Color forEqOrBrighterPixels){
        double thrBrightness = forEqOrBrighterPixels.getBrightness();
        WritableImage forTransps;
        
        PixelReader sReader = aTransparent.getPixelReader();  
        forTransps = new WritableImage((int) (aTransparent.getWidth()), (int) (aTransparent.getHeight()));        
        PixelWriter sWriter = forTransps.getPixelWriter();
        Color imCol;        
        for(int yP = 0; yP < forTransps.getHeight(); yP++){
            for(int xP = 0; xP < forTransps.getWidth(); xP++){ 
                imCol = sReader.getColor((int)(xP), (int)(yP));
                if(imCol.getBrightness() >= thrBrightness){
                    sWriter.setColor(xP, yP, Color.TRANSPARENT);
                } else {
                    sWriter.setColor(xP, yP, imCol);
                }
            }
        }
        return forTransps;
    }
    
    private WritableImage scaleImage(Image preScaleImage, double xScale, double yScale){
        WritableImage forScaling;
        double widthPre, heightPre, widthScaled, heightScaled;
        int xOrig, yOrig;
        
        widthPre = preScaleImage.getWidth();
        heightPre = preScaleImage.getHeight();
        widthScaled = widthPre*xScale;
        heightScaled = heightPre*yScale;
        PixelReader sReader = preScaleImage.getPixelReader();  
        forScaling = new WritableImage((int) widthScaled, (int) heightScaled);        
        PixelWriter sWriter = forScaling.getPixelWriter();     
        for(int yP = 0; yP < (heightScaled - 1.0); yP++){
            for(int xP = 0; xP < (widthScaled -1.0); xP++){ 
                xOrig = (int)(xP/xScale);
                yOrig = (int)(yP/yScale);
                if((xOrig < widthPre) && (yOrig < heightPre)){
                    sWriter.setColor(xP, yP, sReader.getColor((int)(xP/xScale), (int)(yP/yScale)));
                }
            }
        }
        return forScaling;
    }
    
    private void initRepetitionSigns(){
        ImageView lRepButtView = null, rRepButtView = null, segnoButtView = null;
        lRepView = null; rRepView = null;
        ImageView segnoView = null;
        WritableImage lRepButtIm = null, rRepButtIm = null, segnoImage = null;        
                
        segnoImage = transperizeImage(new Image(getClass().getResourceAsStream("sources/SegnoMini.png")),
                Color.WHITE);
        segnoButtView = new ImageView(segnoImage);
        segnoButtView.setFitWidth(segnoImage.getWidth());
        segnoButtView.setFitHeight(segnoImage.getHeight());
        Rectangle frontSegButt = new Rectangle(0.0,0.0,segnoButtView.getFitWidth(),
                segnoButtView.getFitHeight());
        frontSegButt.setFill(Color.TRANSPARENT);
        frontSegButt.setStroke(Color.TRANSPARENT);
        segnoButton.setView(new Group(segnoButtView, frontSegButt));
        segnoButton.setTranslateX(tFLabel.getTranslateX() + 120.0 + 15.0);
        segnoButton.setTranslateY(tFLabel.getTranslateY());  
        segnoButton.setOnMousePressed((mP)->{
            ornamentation = true;
            segnoSeg.setVisible(true);
        });

        segnoView = new ImageView(segnoImage); 
        segnoSeg.setVisible(false);
        segnoSeg.getChildren().add(segnoView);
        segnoSeg.setTranslateX(segnoButton.getTranslateX());
        segnoSeg.setTranslateY(segnoButton.getTranslateY()); 
        
        lRepButtIm = transperizeImage(new Image(getClass().getResourceAsStream("sources/repTheLeft.png")), Color.WHITE);
        lRepButtView = new ImageView(lRepButtIm);
        rRepButtIm = transperizeImage(new Image(getClass().getResourceAsStream("sources/repRighter.png")),
                Color.WHITE);
        rRepButtView = new ImageView(rRepButtIm);
        
        lRepView = new ImageView(lRepButtIm); 
        lRepView.setFitWidth(12.0);
        lRepView.setFitHeight(23.0);
        rRepView = new ImageView(rRepButtIm);   
        rRepView.setFitWidth(12.0);
        rRepView.setFitHeight(23.0);
        
        lRepButtView.setFitWidth(12.0);
        lRepButtView.setFitHeight(23.0);
        rRepButtView.setFitWidth(12.0);
        rRepButtView.setFitHeight(23.0);
        rRepButtView.setTranslateX(lRepButtView.getBoundsInLocal().getWidth() + 5.0);
        Group repsButtGr = new Group(lRepButtView, rRepButtView);
        Rectangle repsButtBack = new Rectangle(0.0,0.0,repsButtGr.getBoundsInLocal().getWidth(),
                repsButtGr.getBoundsInLocal().getHeight());
        repsButtBack.setFill(Color.TRANSPARENT);
        repsButtBack.setStroke(Color.TRANSPARENT);
        repsButtGr.getChildren().add(repsButtBack);
        repsButton.setView(repsButtGr);            
        repsButton.setTranslateX(segnoButton.getTranslateX() + 
                segnoButton.getBoundsInLocal().getWidth() + 10.0);
        repsButton.setTranslateY(tFLabel.getTranslateY());
        repsButton.setOnMousePressed((mP)->{
            ornamentation = true;
            regroupRepViews();            
            repResizing.setWidthExclusive(reps.getBoundsInLocal().getWidth());
            repResizing.setHeightExclusive(reps.getBoundsInLocal().getHeight());
            repResizing.setTranslateXRA(sPiecePane.getBoundsInParent().getMinX());
            repResizing.setTranslateYRA(sPiecePane.getBoundsInParent().getMinY());
            reps.setVisible(true);
            repResizing.setVisible(true);
            actionsPerformed.add(Actions.REPS_SET_VISIBLE);
        });

        reps.setVisible(false);
        
        repResizing.setVisible(false);
        repResizing.setResizableNode(reps);
        
        repsTranslL.setVisible(false);
        repsTranslR.setVisible(false);
        repsTranslL.setOpacity(0.5);
        repsTranslR.setOpacity(0.5);
                  
    }
    
    private void escapeRepPlacement(){
        if(repResizing.isVisible()){
            repResizing.setVisible(false);
        }
        reps.setVisible(false);
    }
    
    private void regroupRepViews(){
        lRepView.setTranslateX(0.0);
        lRepView.setTranslateY(0.0);
        lRepView.setScaleX(1.0); lRepView.setScaleY(1.0);
        rRepView.setTranslateX(lRepView.getBoundsInLocal().getWidth() + 1.0);
        rRepView.setTranslateY(0.0);
        rRepView.setScaleX(1.0); rRepView.setScaleY(1.0);
        repsTranslL.getChildren().remove(lRepView);
        repsTranslR.getChildren().remove(rRepView);
        repsTranslL.setVisible(false);
        repsTranslR.setVisible(false);
        reps.getChildren().removeAll(lRepView, rRepView); //In case already present;
        reps.getChildren().addAll(lRepView, rRepView);
    }
    private void ungroupRepViews(){
        Point2D lRepViewPos, rRepViewPos;
        
        lRepViewPos = lRepView.localToScene(0.0,0.0);        
        rRepViewPos = rRepView.localToScene(0.0,0.0);
        
        lRepView.setTranslateX(0.0);
        lRepView.setTranslateY(0.0);        
        rRepView.setTranslateX(0.0);
        rRepView.setTranslateY(0.0);        
        
        repsTranslL.setScaleX(reps.getScaleX());        
        repsTranslL.setScaleY(reps.getScaleY());        
        repsTranslR.setScaleX(reps.getScaleX());
        repsTranslR.setScaleY(reps.getScaleY());
        
        reps.getChildren().removeAll(lRepView, rRepView);
        repsTranslL.getChildren().removeAll(lRepView, rRepView); //In case already present:
        repsTranslL.getChildren().add(lRepView);
        repsTranslR.getChildren().add(rRepView);
        
        repsTranslL.setTranslateX(repsTranslL.getTranslateX() + 
                (repsTranslL.getParent().sceneToLocal(lRepViewPos).getX() - 
                repsTranslL.getBoundsInParent().getMinX()));
        repsTranslL.setTranslateY(repsTranslL.getTranslateY() + 
                (repsTranslL.getParent().sceneToLocal(lRepViewPos).getY() - 
                repsTranslL.getBoundsInParent().getMinY()));
        repsTranslR.setTranslateX(repsTranslR.getTranslateX() + 
                (repsTranslR.getParent().sceneToLocal(rRepViewPos).getX() - 
                repsTranslR.getBoundsInParent().getMinX()));
        repsTranslR.setTranslateY(repsTranslR.getTranslateY() + 
                (repsTranslR.getParent().sceneToLocal(rRepViewPos).getY() - 
                repsTranslR.getBoundsInParent().getMinY()));
        
        repsTranslL.setVisible(true);
        repsTranslR.setVisible(true);
    }
    
    private void drawSceneImageToPieceContext(Image sceneImage,
            double xScaleImage, double yScaleImage,
            Point2D sceneCoordinates){
        if(sPCont != null){
            Point2D scenePiecePane, shPieceLoc;
            
            scenePiecePane = sPiecePane.screenToLocal(sceneCoordinates);             
            if(sPiecePane.contains(scenePiecePane)){
                shPieceLoc = shrunkPiece.screenToLocal(sceneCoordinates);
                if(shPieceLoc.getX() > shrunkPiece.getWidth()){
                    shrunkPiece.setWidth(shPieceLoc.getX());
                }
                if(shPieceLoc.getY() > shrunkPiece.getHeight()){
                    shrunkPiece.setHeight(shPieceLoc.getY());
                }
                if((xScaleImage != 1.0) || (yScaleImage != 1.0)){                                            
                    sPCont.drawImage(scaleImage(sceneImage, xScaleImage,
                            yScaleImage), shPieceLoc.getX(), shPieceLoc.getY());
                } else {
                    sPCont.drawImage(sceneImage, shPieceLoc.getX(), shPieceLoc.getY());
                }
            }            
            storeShrunkPieceGraphics();
        }
    }
    
    private void mergeSignWithScore(){
            double scX, scY;
            Point2D sceneRepL, sceneRepR, sceneSegno;
            
            generateSPGraphicsRecovery();
            
            if(repsTranslL.isVisible()){
                sceneRepL = lRepView.localToScreen(0.0, 0.0);
                scX = lRepView.getFitWidth()/lRepView.getImage().getWidth();
                scX = repsTranslL.getScaleX() * scX;
                scY = lRepView.getFitHeight()/lRepView.getImage().getHeight();
                scY = repsTranslL.getScaleY() * scY;
                drawSceneImageToPieceContext(lRepView.getImage(),
                            scX,scY,sceneRepL);
                actionsPerformed.add(Actions.REP_SIGN_MERGE);
                repsTranslL.setVisible(false);
            }
            if(repsTranslR.isVisible()){
                sceneRepR = rRepView.localToScreen(0.0,0.0);
                scX = rRepView.getFitWidth()/rRepView.getImage().getWidth();
                scX = repsTranslR.getScaleX() * scX;
                scY = rRepView.getFitHeight()/rRepView.getImage().getHeight();
                scY = repsTranslR.getScaleY() * scY;
                drawSceneImageToPieceContext(rRepView.getImage(),
                            scX, scY,sceneRepR);
                actionsPerformed.add(Actions.REP_SIGN_MERGE);
                repsTranslR.setVisible(false);
            }
            if(segnoSeg.isVisible()){
                sceneSegno = segnoSeg.localToScreen(0.0, 0.0);
                drawSceneImageToPieceContext(
                        ((ImageView)segnoSeg.getChildren().get(0)).getImage(),
                        1.0, 1.0, sceneSegno);                
                actionsPerformed.add(Actions.SEGNO_MERGE);
                segnoSeg.setVisible(false);
            }
        
    }
    
    private void drawSegno(){
        
        double[] xDASegnoSegCanv = new double[]{144.49139693707195, 144.41681502415884, 144.19355297774393, 143.82305873787118, 143.30773510185634, 142.6509241412249, 141.85688552709576, 140.93076890457928, 139.87858049535248, 138.7071441450073, 137.42405706779277, 136.03764057576564, 134.5568861118885, 132.99139693707195, 131.35132584934217, 129.64730933905025, 127.89039860715036, 126.09198789392099, 124.26374058294437, 122.41751355958854, 120.56528031455537, 118.71905329119954, 116.89080598022292, 115.09239526699355, 113.33548453509366, 111.63146802480173, 109.99139693707195, 108.4259077622554, 106.94515329837827, 105.55873680635113, 104.2756497291366, 103.10421337879143, 102.05202496956463, 101.12590834704815, 100.33186973291902, 99.67505877228757, 99.15973513627273, 98.78924089639997, 98.56597884998507, 98.56597884998507, 98.60533414586371, 98.72332614804395, 98.91973333887029, 99.19418698436425, 99.54617182648349, 99.97502705046497, 100.47994752543605, 101.05998531596299, 101.71405146170116, 102.44091802180378, 103.23922038025319, 104.10745980778427, 105.04400627559244, 106.0471015155423, 107.11486232113214, 108.24528408301762, 109.43624455245617, 110.6855078256076, 111.99072854120948, 113.34945628374805, 114.75914018385731, 116.21713370730993, 117.72069962360831, 119.26701514484921, 120.85317722521279, 122.47620801112805, 124.1330604318814, 125.82062392017428, 127.53573025188723, 129.27515949409064, 131.0356460501314, 132.8138847904504, 134.60653725761722, 136.41023793393515, 138.22160055984966, 140.0372244912959, 141.85370108405357, 143.6676200931197, 145.47557607508736, 147.27417478151006, 149.06003953124844, 150.8298175498362, 152.5801862639638, 154.30785953926318, 156.00959384968075, 157.68219436685956, 159.32252095809508, 160.92749408160694, 162.49410056805573, 164.0193992774549, 165.50052662085284, 166.93470193642224, 168.31923270986215, 169.65151962931225, 170.92906146528924, 172.1494597664838, 173.31042336260225, 174.40977266579978, 175.4454437626282, 176.41549228881854, 176.41549228881854, 177.4155623674667, 178.3834461339734, 179.31840210140842, 180.21971400852783, 181.08669136849585, 181.9186699978602, 182.71501252537712, 183.4751088802957, 184.19837675972724, 184.88426207474254, 185.53223937485416, 186.1418122505595, 186.71251371363508, 187.24390655489253, 187.73558367912017, 188.18716841695527, 188.5983148134468, 188.9687078930882, 189.29806390111764, 189.58613052089993, 189.83268706722333, 190.03754465536542, 190.2005463457955, 190.32156726440405, 190.400514698169, 190.43732816618058, 190.43197946597684, 190.38447269514813, 190.29484424819861, 190.16316278866418, 189.98952919651106, 189.77407649085092, 189.5169697280378, 189.21840587521888, 189.21840587521888, 189.19082718278878, 189.1081073352766, 188.97029501246539, 188.77747131549563, 188.5297497191384, 188.22727600501605, 187.87022817581146, 187.45881635051558, 186.99328264077394, 186.47390100840732, 185.90097710418684, 185.2748480879627, 184.59588243024848, 183.86447969538017, 183.08107030637655, 182.24611529163985, 181.36010601364512, 180.42356387977838, 179.43704003549385, 178.4011150399706, 177.31639852445903, 176.18352883351952, 175.0031726493628, 173.77602459951493, 172.50280684803533, 171.18426867053108, 169.8211860132161, 168.41436103627427, 166.96462164179638, 165.47282098656888, 163.9398369799991, 162.36657176747485, 160.75395119946143, 159.1029242866473, 157.4144626414618, 155.68955990629104, 153.92923116872885, 152.1345123642093, 150.30645966636814, 148.44614886549618, 146.5546747354474, 144.63315038937606, 142.68270662468058, 140.70449125754112, 138.69966844744147, 136.66941801207292, 134.61493473302426, 132.5374276526643, 130.43811936263285, 128.31824528435902, 126.17905294202717, 124.02180122842219, 121.84775966408438, 119.65820765020896, 117.45443371573185, 115.23773475904375, 113.00941528477836, 110.7707866361252, 108.52316622311719, 106.2678767473484, 104.0062454235781, 101.73960319867774, 99.46928396838314, 97.19662379230988, 94.92296010769633, 92.6496309423352, 92.6496309423352, 90.97985821876239, 89.31064579014799, 87.64255376344238, 85.97614186964347, 84.31196927597773, 82.65059439827075, 80.9925747135701, 79.33846657308277, 77.6888250154908, 76.04420358070706, 74.4051541241339, 72.77222663148746, 71.14596903424876, 69.52692702580464, 67.91564387833932, 66.31266026053879, 64.71851405616792, 63.13374018358343, 61.55887041624078, 59.994433204256666, 58.44095349708692, 56.89895256737873, 55.36894783605692, 53.851452698702246, 52.346976353280866, 50.85602362928216, 49.379094818322244, 47.91668550627031, 46.469286406954154, 45.03738319750016, 43.62145635536359, 42.22198099710363, 40.83942671895733, 39.474257439265614, 38.126931242804915, 36.79790022707601, 35.487610350601926, 34.1965012832859, 32.92500625887942, 31.673551929609914, 30.442558223016817, 29.232438201044374, 28.043597921437936, 26.87643630149057, 25.731344984185853, 24.608708206781216, 23.50890267187674, 22.432297421011867, 21.379253710832927, 20.350124891872838, 19.34525628998381, 18.36498509046271, 17.40964022490789, 16.479542260845847, 15.575003294164048, 14.69632684438676, 13.843807752828525, 13.01773208365978, 12.21837702791737, 11.446010810492908, 10.700892600129237, 9.983272422456139, 9.293391076093485, 8.631480051851327, 7.997761455052455, 7.39244793100471, 6.815742593647485, 6.267838957396464, 5.7489208722096805, 5.25916246189621, 4.798728065688692, 4.367772183099078, 3.966439422076064, 3.594864450481566, 3.2531719509028107, 2.9414765788148998, 2.6598829241078192, 2.408485475991256, 2.187368591288589, 1.9966064661308565, 1.8362631110599068, 1.706392329549999, 1.6070376999533096, 1.538232560877816, 1.5, 1.5, 1.5116204035793999, 1.54647998732014, 1.6045738704581822, 1.6858939191457694, 1.7904287475906813, 1.9181637196501242, 2.0690809508798793, 2.2431593110388803, 2.4403744270468906, 2.66069868639795, 2.9041012410255007, 3.170548011622259, 3.4600016924114243, 3.772421756369795, 4.107764460902445, 4.465982853966807, 4.847026780646729, 5.250842890174681, 5.677374643401777, 6.126562320713447, 6.598343030391334, 7.0926507174189055, 7.609416172729425, 8.148567042896843, 8.710027840265525, 9.293719953519712, 9.89956165868989, 10.52746813059548, 11.17735145472102, 11.849120639525495, 12.542681629182027, 13.257937316747075, 13.994787557756524, 14.753129184246973, 15.532856019200608, 16.33385889141141, 17.156025650770175, 17.999241183967115, 18.863387430608896, 19.748343399748705, 20.653985186826446, 21.580185991016833, 22.526816132982958, 23.4937430730331, 24.48083142967789, 25.48794299858514, 26.514936771930365, 27.561668958139705, 28.627993002022038, 29.71375960528883, 30.818816747457618, 31.943009707136753, 33.08618108368819, 34.248170819265624, 35.428816221224224, 36.62795198490005, 37.8454102167542, 39.0810204578803, 40.334609707870754, 41.60600244903878, 42.89502067099306, 44.20148389556141, 45.525209202059784, 46.86601125290366, 48.22370231955716, 49.59809230881763, 50.98898878943078, 52.39619701903371, 53.81951997142073, 55.25875836412985, 56.713710686344484, 58.18417322710752, 59.6699401038432, 61.170803291183404, 62.686552650093404, 64.21697595729438, 65.76185893497643, 67.32098528080098, 68.89413669818498, 70.48109292686581, 72.08163177373956, 72.08163177373956, 74.179399956123, 76.27279447797014, 78.36133316748669, 80.44453497132292, 82.52192006537427, 84.5930099653001, 86.6573276367319, 88.71439760515, 90.76374606539832, 92.80490099081703, 94.83739224196415, 96.86075167490219, 98.87451324902725, 100.87821313441054, 102.87138981863359, 104.85358421308774, 106.82433975871675, 108.78320253117556, 110.72972134538247, 112.66344785944159, 114.58393667790881, 114.58393667790881, 116.25632778462523, 117.9258541248006, 119.59205197370284, 121.2544585311158, 122.91261204988575, 124.56605196417593, 126.21431901739186, 127.85695538974403, 129.49350482541092, 131.12351275926846, 132.746526443148, 134.36209507159208, 135.96976990706878, 135.96976990706878, 137.963578868573, 139.95651908067407, 141.9477221725034, 143.9363205300965, 145.9214476744329, 147.90223863898098, 149.8778303465848, 151.84736198552685, 153.8099753846035, 155.76481538705116, 157.71103022315776, 159.6477718813985, 161.57419647793495, 163.4894646243148, 165.3927417932133, 167.28319868205597, 169.16001157436574, 171.02236269867535, 172.86944058484949, 174.70044041766164, 176.51456438747073, 178.31102203784576, 180.0890306099858, 181.8478153837873, 183.58661001540656, 185.3046568711751, 187.00120735771725, 188.67552224813085, 190.32687200408486, 191.95453709369724, 193.55780830505125, 195.1359870552161, 196.6883856946356, 198.21432780675366, 199.71314850274462, 201.18419471122093, 202.62682546279154, 204.04041216934786, 205.4243388979537, 206.77800263922234, 208.10081357006118, 209.3921953106725, 210.65158517569444, 211.8784344193773, 213.07220847468443, 214.23238718621587, 215.35846503685195, 216.44995136801947, 217.50637059348253, 218.52726240656727, 219.51218198072786, 220.46070016336807, 221.37240366283334, 222.24689522849098, 223.08379382382225, 223.8827347924489, 224.643370017022, 225.36536807090545, 226.04841436258596, 226.69221127274807, 227.2964782839539, 227.86095210287135, 228.38538677499673, 228.86955379182348, 229.31324219040835, 229.71625864529278, 230.07842755273987, 230.3995911072484, 230.3995911072484, 230.37138452630518, 230.2868056803211, 230.14597720053723, 229.94910327478578, 229.6964693514377, 229.38844172553001, 229.02546700767243, 228.60807147650507, 228.13686031564487, 227.61251673622667, 227.035800986313, 226.40754924860653, 225.7286724280657, 225.00015483117875, 224.22305273881443, 223.39849287471486, 222.52767077185433, 221.61184903903052, 220.6523555302026, 219.65058141922987, 218.60797918280275, 217.52606049449037, 216.40639403295802, 215.25060320753414, 214.0603638044206, 212.8374015569641, 211.58348964350694, 210.3004461164486, 208.990131266243, 207.65444492415577, 206.29532370768993, 204.91473821267607, 203.51469015609678, 202.0972094737881, 200.6643513772268, 199.2181933736706, 197.7608322539703, 196.29438105242446, 194.82096598308112, 193.34272335693117, 191.8617964844608, 191.8617964844608, 191.03578794571195, 190.3962353280342, 189.57766948387325, 188.58286018093474, 187.4151735390003, 186.07856064004386, 184.5775441590206, 182.91720306056675, 181.10315541339094, 179.14153938050777, 177.03899244963782, 174.80262897404953, 172.44001609984014, 169.9591481611085, 167.368419629659, 164.67659671076638, 161.8927876811116, 159.02641206925716, 156.08716878294376, 153.08500329105664, 150.0300739713074, 146.93271773749893, 143.80341506268144, 140.65275451654907, 137.49139693707195, 134.33003935759484, 134.33003935759484, 132.9461656184685, 131.56903397309281, 130.205353668415, 128.86176841780298, 127.54482403354086, 126.26093653629016, 125.01636089688145, 123.81716056272569, 122.6691779173076, 121.57800581668096, 121.57800581668096, 121.6446387371858, 121.8422683945098, 122.16416474770347, 122.59936600253081, 123.13305190222536, 123.74704841355833, 123.74704841355833, 123.82937092619557, 124.07586692820996, 124.48512451282454, 125.0547994896566, 125.78162881202843, 126.6614492674023, 127.68922132388604, 128.85905799621435, 130.16425856586773, 131.5973469621813, 133.15011458460037, 134.8136673208013, 136.57847649136136, 138.43443342917146, 140.37090738096595, 142.37680639931477, 144.44064087629363, 146.55058935491763, 148.69456624137683, 150.8602910302227, 153.0353586459911, 155.20731049835172, 157.3637058437845, 159.49219304503254, 161.5805803201614, 163.61690557598206, 163.61690557598206, 164.96483958523538, 166.21583798455788, 167.80193403524754, 169.67214930519094, 171.76637353679394, 174.01729664011918, 176.3525720945603, 176.3525720945603, 174.1553388337286, 171.9753019022819, 169.82952304501202, 167.73479589083172, 165.70751451985836, 163.7635451575095, 161.91810199977658, 160.18562814151335, 158.57968253963531, 157.11283389589892, 155.7965622897682, 154.64116933122807, 153.6556975367189, 152.8478595591847, 152.223977826107, 151.788935057937, 151.54613605418865, 151.4974810462653, 151.64335082557272, 151.9826037633103, 152.51258474526423, 153.22914595167595, 154.12667931955548, 155.1981604333787, 156.43520350066206, 157.828126982159, 159.36602936303143, 161.036874471984, 162.82758568062343, 164.7241482458063, 166.71171899400542, 168.7747424892682, 170.8970727755933, 173.06209974092883, 175.25287911381884, 177.45226507530185, 179.64304444819186, 181.8080714135274, 183.9304016998526, 183.9304016998526, 185.2194562503695, 186.49683692326198, 187.7509755613488, 188.97051449091555, 190.14440937856557, 191.26202925041434, 192.31325276783457, 193.28855988786654, 194.1791180782012, 194.9768623059606, 194.9768623059606, 195.5173662325659, 195.97211957377522, 196.33890681702167, 196.61594101172545, 196.80187247513754, 196.89579536785777, 196.89725210699396, 196.8062355954595, 196.62318925654972, 196.3490048736278, 195.98501824544667, 195.5330026782708, 194.99516034650708, 194.3741115639324, 193.67288201778751, 192.89488802793414, 192.04391990288696, 191.12412347381235, 190.13997989645486, 189.09628381939808, 187.9981200250171, 186.85083865693116, 185.6600291546414, 185.6600291546414, 184.81540119184433, 183.90744757942946, 182.93825663980135, 181.91005754317928, 180.82521518043438, 179.68622472376535, 178.495705887722, 177.25639690377784, 175.97114822231015, 174.64291595647285, 173.27475508304087, 171.8698124158666, 170.43131936810641, 168.9625845198675, 167.46698600836692, 165.94796375810824, 164.40901156894483, 162.8536690802281, 161.28551362952385, 159.70815202462074, 159.70815202462074, 157.9072825753135, 156.10774539288707, 154.3108717586233, 152.51799098333527, 150.73042942395512, 148.94950950230748, 147.1765487267936, 145.4128587177106, 143.6597442369266, 141.91850222263008, 140.19042082986618, 138.47677847757188, 136.77884290281304, 135.0978702229238, 133.4351040062428, 131.79177435213205, 130.16909698096117, 128.56827233472814, 126.99048468898332, 125.436901276713, 123.90867142483125, 122.40692570391809, 120.93277509183383, 119.48731015182739, 118.07160022574806, 116.68669264295545, 115.33361194551514, 114.01335913025196, 112.72691090822082, 111.47521898214563, 110.25920934235774, 109.07978158175672, 107.93780823029999, 106.83413410951266, 105.76957570749676, 104.7449205749, 103.7609267422938, 102.81832215938857, 101.91780415650317, 101.06003892868637, 100.24566104287226, 99.47527296843225, 98.74944463147494, 98.06871299321904, 97.43358165275481, 96.84452047448599, 96.30196524052815, 95.80631732832126, 95.3579434136941, 94.95717519960101, 94.60430917073097, 94.29960637417082, 94.04329222628519, 93.8355563459553, 93.67655241430089, 93.56639806098792, 93.50517477720791, 93.49292785539046, 93.52966635569749, 93.61536309931967, 93.74995468858344, 93.93334155385207, 94.16538802718662, 94.4459224427111, 94.77473726360984, 95.15158923566162, 95.57619956719634, 96.04825413534286, 96.56740371841408, 97.1332642542585, 97.74541712438509, 98.40340946365416, 99.10675449530265, 99.85493189105671, 100.64738815606688, 101.48353703837745, 102.36275996263112, 103.2844064876848, 104.24779478780056, 105.25221215705426, 106.29691553658813, 107.3811320643199, 108.50405964669835, 109.66486755208564, 110.86269702532434, 112.096661923037, 113.36584936918712, 114.66932043041629, 116.00611081065881, 117.3752315645188, 118.77566982888357, 120.20638957223025, 121.66633236107225, 123.15441814297935, 124.66954604559021, 126.21059519102812, 127.77642552511577, 129.36587866077764, 130.97777873500365, 132.61093327874244, 134.26413409907866, 135.93615817304362, 137.62576855239638, 139.33171527870672, 141.052736308063, 142.7875584447213, 144.53489828300337, 146.29346315674923, 148.06195209561895, 149.8390567875399, 151.62346254658314, 153.41384928555647, 155.20889249259312, 157.00726421101353, 157.00726421101353, 159.38400018526295, 161.75990999165583, 164.13416774951668, 166.50594815243198, 168.87442675513205, 171.23878026007264, 173.598186803619, 175.9518262417298, 178.2988804350448, 180.63853353327465, 182.9699722587959, 185.29238618935074, 187.60496803975468, 189.9069139425144, 192.19742372725636, 194.47570119887098, 196.74095441427528, 198.99239595769654, 201.22924321438325, 203.45071864264662, 205.6560500441385, 207.8444708322728, 210.01522029869523, 212.16754387771005, 214.300693408572, 216.41392739555147, 218.50651126568266, 220.57771762410596, 222.6268265069143, 224.65312563141788, 226.6559106437369, 228.63448536363978, 230.58816202653998, 232.5162615225662, 234.41811363262616, 236.29305726137795, 238.14044066703138, 239.95962168789708, 241.74996796560595, 243.51085716492048, 245.24167719006266, 246.94182639748203, 248.6107138049906, 250.2477592971918, 251.852393827132, 253.42405961410532, 254.96221033754057, 256.46631132690663, 257.93583974756643, 259.3702847825176, 260.76914780995617, 262.1319425765995, 263.4581953667116, 264.7474451667696, 265.9992438257144, 267.213156210731, 268.38876035850353, 269.5256476218916, 270.62342281197914, 271.68170433544435, 272.70012432720364, 273.6783287782837, 274.6159776588778, 275.51274503654133, 276.3683191894886, 277.1824027149489, 277.95471263254547, 278.68498048266144, 279.3729524197579, 280.01838930061194, 280.62106676744463, 281.18077532590894, 281.6973204179113, 282.1705224892413, 282.6002170519856, 282.9862547417049, 283.3285013693543, 283.6268379679277, 283.88116083381215, 284.09138156283495, 284.25742708099426, 284.3792396698599, 284.456776986636, 284.4900120788807, 284.4900120788807, 285.12646651028274, 285.7240599850122, 286.2725675130321, 286.7626039784627, 287.1857847215173, 287.53486900235396, 287.8038838921433, 287.98822647153577, 288.08474258788874, 288.09178082369203, 288.0092207527798, 287.83847500085517, 287.5824650750733, 287.5824650750733, 287.293859215977, 286.97951519365404, 286.6396893915976, 286.2746589766065, 285.8847216727229, 285.4701955184048, 285.0314186071298, 284.56874881164026, 284.0825634920577, 283.5732591881025, 283.04125129567, 282.4869737280284, 281.91087856191234, 281.3134356688023, 280.6951323316915, 280.0564728476505, 279.3979781165151, 278.7201852160332, 278.0236469638164, 277.3089314664543, 276.57662165615784, 275.82731481531243, 275.0616220893253, 274.28016798816645, 273.48358987700976, 272.67253745638857, 271.8476722322904, 271.0096669766235, 270.15920517849474, 269.29698048674516, 268.4236961442008, 267.5400644140976, 266.6468059991489, 265.74464945373074, 264.8343305896616, 263.91659187606433, 262.9921818337974, 262.9921818337974, 261.5690514742257, 260.1287186054416, 258.6718500958599, 257.19912046983177, 255.7112115953422, 254.20881236830837, 252.69261839362343, 251.16333166309346, 249.62166023041925, 248.06831788336956, 246.50402381330002, 244.92950228217103, 243.34548228721633, 241.7526972234216, 240.151884543964, 238.54378541877554, 236.92914439138326, 235.3087090341893, 233.68322960234752, 232.05345868639813, 230.42015086382185, 228.78406234967292, 227.14595064645493, 225.50657419339973, 223.86669201531367, 223.86669201531367, 221.51673512559728, 219.16146251757596, 216.80334454697345, 214.444854553906, 212.08846626869024, 209.73665121724082, 207.39187612878243, 205.05660034859045, 202.73327325847777, 200.42433170773108, 198.132197457193, 195.85927463916937, 195.85927463916937, 193.0273809637464, 190.19725830103482, 187.37277225816774, 184.55778074333364, 181.7561286963372, 178.9716428368754, 176.20812643769716, 173.46935412979133, 170.75906674669466, 168.08096621496247, 165.43871049778295, 162.83590859863955, 160.2761156318453, 157.76282796668602, 155.29947845179845, 152.88943172631355, 150.53597962416336, 148.24233667783187, 146.01163572769053, 143.84692364291516, 141.7511571598286, 139.72719884335572, 137.77781317710264, 135.9056627874055, 134.11330480650327, 132.40318737980203, 132.40318737980203, 130.5999720563475, 128.79780170653748, 126.99772069844857, 125.20077218937149, 123.4079975212959, 121.62043561744667, 119.83912238022162, 118.06509009088074, 116.29936681133245, 114.54297578836645, 112.79693486067617, 111.06225586901519, 109.33994406982981, 107.63099755270753, 105.93640666197768, 104.25715342280182, 102.59421097208417, 100.94854299453334, 99.32110316420153, 97.7128345918245, 96.12466927828427, 94.55752757450864, 93.01231764812258, 91.48993495716024, 89.99126173114144, 88.5171664598148, 87.06850338986317, 85.64611202986225, 84.25081666378071, 82.88342587330351, 81.54473206925309, 80.23551103238367, 78.95652146381133, 77.70850454534263, 76.49218350995511, 75.30826322268024, 74.15742977212926, 73.0403500729019, 71.95767147910465, 70.91002140920568, 69.89800698244221, 68.92221466699152, 67.98320994010919, 67.08153696043178, 66.21771825263363, 65.39225440462121, 64.60562377743884, 63.85828222805662, 63.1506628451981, 62.48317569836274, 61.85620760018901, 61.270121882293324, 60.7252581847182, 60.22193225910797, 59.76043578572967, 59.34103620444233, 58.96397655971384, 58.62947535977571, 58.3377264499959, 58.08889890054462, 57.883136908416986, 57.7205597138701, 57.60126153132279, 57.52531149475749, 57.49275361765683, 57.5036067674979, 57.557864654817934, 57.6554958368593, 57.79644373579089, 57.980626671495145, 58.20793790890207, 58.47824571984313, 58.791393459388075, 59.14719965662198, 59.54545811980847, 59.98593805587882, 60.46838420417839, 60.99251698439116, 61.55803265855832, 62.16460350709622, 62.81187801871158, 63.49948109410445, 64.2270142633405, 64.99405591676611, 65.80016154933458, 66.64486401819829, 67.52767381342119, 68.44807934165271, 69.40554722259901, 70.39952259811946, 70.39952259811946, 72.36096228514202, 74.33035593245052, 76.30651725095231, 78.28825587496272, 80.27437807923639, 82.26368749802339, 84.25498584571505, 86.24707363864604, 88.23875091761897, 90.22881797071443, 92.21607605595318, 94.199328123373, 96.17737953608707, 98.14903878988832, 100.11311823096753, 102.06843477131099, 104.01381060134872, 105.9480738994232, 107.87005953765026, 109.77860978374918, 111.67257499841685, 113.55081432782822, 115.41219639084272, 117.25559996050742, 119.07991463944109, 120.8840415286968, 122.66689388969723, 124.42739779884482, 126.16449279441451, 127.87713251533512, 129.56428533147857, 131.2249349650748, 132.85808110288053, 134.4627399987296, 136.03794506610484, 137.5827474603749, 139.09621665034103, 140.57744097875536, 142.02552821146935, 143.43960607488123, 144.81882278136186, 146.16234754233972, 147.4693710687361, 148.73910605845163, 149.9707876706069, 151.16367398625448, 152.3170464552826, 153.43021032924298, 154.50249507984233, 155.53325480284184, 155.53325480284184, 156.6454143601071, 157.70199122391352, 158.7005552336159, 159.6388096599926, 160.51459648779598, 161.32590137925717, 162.07085830712742, 162.74775384659995, 163.35503111624246, 163.89129335887458, 164.35530715415325, 164.74600525548, 165.06248904470146, 165.06248904470146, 165.27443668609988, 165.07419941404237, 164.82012890865929, 164.4644356912304, 164.00830133885302, 163.4532410839746, 162.80109878094152, 162.05404078090123, 161.21454873540154, 161.21454873540154, 161.0761138266182, 160.66275242293455, 159.98026721235885, 159.03823879122172, 157.84989117360385, 156.43190615516676, 154.80418913730773, 152.98958969895784, 151.0135808385803, 148.90390138911027, 146.69016662554372, 144.4034525313947, 142.07585956000594, 142.07585956000594, 143.30985815682186, 144.3726603477885, 145.24161434938924, 145.89819991571528, 146.328423065351, 146.52311433879424, 146.47812422956486, 146.1944116237313, 145.67802336291925, 144.93996536638053};
        double[] yDASegnoSegCanv = new double[]{67.49933581723144, 66.21187071776382, 64.93275532350725, 63.670285188630544, 62.43264791640786, 61.22787005947026, 60.06376506453114, 58.94788259918664, 57.88745958942377, 56.889373285378724, 55.960096659730425, 55.10565642798895, 54.331593962932914, 53.64292935668044, 53.044128863465346, 52.539075934264815, 52.13104603112947, 51.82268538455514, 51.61599383166259, 51.51231184648634, 51.51231184648634, 51.61599383166259, 51.82268538455514, 52.13104603112947, 52.539075934264815, 53.044128863465346, 53.64292935668044, 54.33159396293297, 55.10565642798895, 55.960096659730425, 56.889373285378724, 57.88745958942377, 58.94788259918664, 60.06376506453114, 61.22787005947026, 62.432647916407916, 63.670285188630544, 64.93275532350731, 66.21187071776387, 66.21187071776387, 67.56720468935453, 68.91999416302957, 70.2676994179024, 71.60779027817597, 72.9377508632835, 74.25508431119124, 75.55731746599622, 76.842005521018, 78.10673660866775, 79.34913632847747, 80.56687220478909, 81.75765806573355, 82.91925833527972, 84.04949223029473, 85.14623785473651, 86.20743618329192, 87.23109492698148, 88.215292273474, 89.15818049508817, 90.05798941770831, 90.9130297441015, 91.72169622539622, 92.48247067476984, 93.19392481768546, 93.85472297332808, 94.4636245622059, 95.01948643520876, 95.5212650197509, 95.96801827896968, 96.3589074803013, 96.69319877011338, 96.97026455143879, 97.18958466222313, 97.35074735187465, 97.45345005428311, 97.49749995585597, 97.48281435750567, 97.40942082990864, 97.27745716174428, 97.08717110101082, 96.83891988990484, 96.53316959413581, 96.1704942279369, 95.75157467641287, 95.27719741724968, 94.74825304418471, 94.16573459501006, 93.53073568724795, 92.84444846499792, 92.10816136081098, 91.3232566767922, 90.49120798947257, 89.61357738332362, 88.69201251810654, 87.72824353556331, 86.72407981125644, 85.68140655765524, 84.60218128484667, 83.48843012551424, 82.34224403108612, 82.34224403108612, 81.55064679993728, 80.71825488777199, 79.84570598244608, 78.933668535722, 77.98284125117357, 76.99395254891607, 75.96776000756967, 74.9050497838856, 73.80663601047883, 72.67336017212818, 71.50609046112254, 70.30572111214707, 69.07317171721752, 67.80938652118965, 66.51533369838165, 65.19200461086473, 63.84041304898949, 62.461594454730744, 61.05660512844429, 59.62652141964543, 58.17243890242713, 56.69547153615048, 55.19675081205099, 53.677424886413576, 52.138657700979536, 50.581628091262814, 49.00752888345352, 47.41756598060567, 45.812957438805256, 44.194932534028794, 42.56473082040611, 40.92360118060947, 39.27280086909553, 37.6135945489346, 37.6135945489346, 36.73757013406504, 35.86206125059181, 34.98758312652649, 34.114650383289074, 33.24377673285892, 32.37547467546028, 31.51025519796127, 30.648627473163288, 29.791098560158048, 28.938173105928627, 28.09035304836948, 27.24813732090172, 26.412021558855372, 25.58249780779346, 24.76005423394787, 23.945174836938804, 23.138339164945933, 22.340022032499007, 21.550693241054546, 20.77081730252246, 20.000853165905596, 19.24125394721267, 18.49246666280453, 17.75493196632948, 17.029083889402898, 16.31534958618431, 15.614149082001518, 14.925895026169883, 14.250992449152648, 13.58983852420431, 12.942822333638617, 12.310324639857242, 11.692717661275367, 11.09036485327465, 10.503620694313668, 9.932830477320806, 9.378330106493138, 8.840445899619908, 8.319494396048242, 7.815782170402656, 7.329605652169164, 6.861250951249701, 6.41099368958902, 5.97909883897438, 5.565820565102115, 5.171402078003894, 4.7960754889198824, 4.4400616737038945, 4.103570142840113, 3.7867989181482358, 3.4899344162498664, 3.2131513388640087, 2.9566125699970485, 2.7204690800871845, 2.5048598371596995, 2.309911725045936, 2.135739468713382, 1.9824455667510392, 1.8501202310503118, 1.7388413337159818, 1.6486743612395571, 1.579672375960854, 1.5318759848415766, 1.505313315568344, 1.5, 1.5159391649683585, 1.5159391649683585, 1.5295262203250104, 1.5702828272364968, 1.6381953097569522, 1.733240879742084, 1.8553876444961475, 2.004594617473117, 2.1808117320300084, 2.3839798582270078, 2.6140308226681555, 2.8708874313775823, 3.154463495701634, 3.4646638612297807, 3.801384439723961, 4.164512244045227, 4.553925426067224, 4.969493317562126, 5.411076474046695, 5.87852672157328, 6.371687206449565, 6.890392447871136, 7.4344683934490945, 8.003732477613198, 8.59799368287247, 9.217052603910986, 9.860701514499056, 10.528724437195876, 11.220897215821196, 11.936987590671151, 12.676755276453548, 13.43995204291599, 14.226321798139793, 15.035600674472164, 15.867517117067393, 16.721791975007534, 17.59813859497183, 18.49626291742402, 19.41586357528405, 20.356631995052737, 21.318252500353765, 22.300402417859516, 23.30275218556477, 24.324965463371598, 25.366699245948837, 26.4276039778282, 27.507323670698042, 28.60549602285613, 29.721752540780017, 30.8557186627761, 32.007013884664445, 33.17525188745708, 34.36004066698854, 35.560982665453196, 36.777674904806474, 38.00970912198471, 39.25667190589871, 40.51814483615442, 41.79370462345429, 43.08292325163325, 44.385368121279726, 45.70060219489528, 47.02818414354323, 48.36766849493745, 49.718605782920974, 51.080542698285456, 52.453022240879534, 53.83558387295625, 55.22776367370693, 56.629094494930825, 58.0391061177873, 59.45732541057873, 60.883276487510216, 62.3164808683743, 63.75645763910552, 65.20272361315193, 66.65479349360913, 68.11218003606251, 69.57439421208295, 71.04094537332082, 72.5113414161442, 73.98508894676485, 75.46169344679726, 76.94065943919526, 78.42149065451014, 79.90369019741439, 81.3867607134361, 81.3867607134361, 82.57133921126467, 83.75575185374817, 84.93983280876301, 86.12341629062621, 87.30633658330686, 88.48842806362859, 89.66952522445882, 90.84946269788185, 92.02807527835205, 93.20519794582509, 94.38066588886238, 95.55431452770682, 96.725979537326, 97.8954968704196, 99.06270278038807, 100.227433844259, 101.38952698556852, 102.54881949719396, 103.70514906413462, 104.85835378623796, 106.00827220086768, 107.1547433055103, 108.2976065803175, 109.43670201058086, 110.57187010913589, 111.70295193869214, 112.8297891340863, 113.95222392445521, 115.07009915532603, 116.18325831061918, 117.291545534563, 118.39480565351522, 119.4928841976892, 120.58562742278156, 121.67288233149822, 122.75449669497596, 123.83031907409617, 124.90019884068829, 125.96398619861958, 127.02153220476822, 128.07268878987736, 129.1173087792866, 130.1552459135379, 131.1863548688542, 132.2104912774862, 133.2275117479254, 134.23727388498116, 135.2396363097173, 136.2344586792472, 137.22160170638313, 138.20092717913855, 139.17229798007918, 140.13557810552118, 141.0906326845734, 142.03732799802083, 142.97553149704686, 143.90511182179193, 144.82593881974543, 145.73788356396835, 146.6408183711451, 147.53461681946027, 148.41915376629947, 149.29430536577087, 150.15994908604483, 151.0159637265101, 151.86222943474337, 152.6986277232898, 153.5250414862531, 154.34135501569136, 155.1474540178179, 155.94322562900368, 156.72855843157936, 157.5033424694355, 158.2674692634174, 159.02083182651393, 159.76332467883628, 160.4948438623873, 161.2152869556163, 161.9245530877593, 162.62254295296265, 163.3091588241864, 163.3091588241864, 163.88898871248034, 164.48294893046358, 165.0909026712127, 165.7127099046745, 166.34822740991956, 166.99730880812984, 167.6598045963142, 168.3355621817438, 169.02442591709837, 169.72623713631685, 170.44083419114264, 171.16805248835607, 171.90772452768562, 172.65967994038766, 173.4237455284881, 174.19974530467437, 174.98750053283152, 175.78682976920965, 176.5975489042164, 177.4194712048228, 178.25240735757347, 178.25240735757347, 178.4292803280573, 178.63049768000337, 178.8560035236302, 179.10573522282675, 179.37962341255087, 179.6775920180952, 179.9995582762183, 180.34543275813235, 180.71511939434293, 181.10851550133304, 181.52551181008437, 181.96599249642748, 182.4298352132132, 182.4298352132132, 182.44183275076665, 182.47782013581798, 182.5377816878181, 182.6216912801102, 182.72951235131376, 182.86119792125504, 183.01669061143775, 183.19592267004384, 183.39881600145492, 183.62528220028008, 183.8752225898764, 184.1485282653445, 184.44508014098108, 184.76474900216726, 185.10739556167061, 185.47287052033573, 185.86101463213737, 186.2716587735677, 186.70462401732743, 187.15972171028864, 187.63675355569535, 188.1355116995661, 188.65577882126075, 189.19732822817218, 189.75992395450186, 190.34332086407522, 190.94726475715362, 191.57149248119492, 192.2157320455155, 192.8797027398026, 193.56311525642673, 194.26567181649972, 194.9870662996238, 195.7269843772757, 196.48510364976676, 197.26109378672032, 198.05461667100406, 198.86532654605583, 199.69287016653783, 200.53688695225406, 201.39700914526327, 202.27286197011995, 203.16406379717273, 204.07022630884944, 204.99095466885626, 205.92584769421683, 206.87449803007758, 207.83649232720143, 208.8114114220739, 209.79883051954255, 210.79831937791027, 211.8094424964013, 212.83175930491961, 213.86482435601545, 214.90818751897734, 215.96139417596441, 217.02398542009428, 218.09549825539955, 219.17546579856582, 220.26341748236382, 221.35887926068688, 222.46137381510346, 223.57042076283642, 224.68553686607697, 225.80623624254247, 226.93203057718722, 228.0624293349723, 229.19693997460377, 229.19693997460377, 230.88356357122393, 232.5677417248719, 234.24703253823373, 235.91900120017192, 237.58122351596853, 239.23128942217556, 240.86680648097695, 242.4854033489944, 244.08473321550855, 245.6624772051107, 247.21634773984943, 248.7440918560011, 250.24349447065083, 251.712381593352, 253.1486234782035, 254.55013771177835, 255.914892232424, 257.24090827655743, 258.5262632476829, 259.7690935039735, 260.96759706037335, 262.1200362013037, 263.2247400001846, 264.2801067421183, 265.284606246223, 266.23678208424906, 267.13525369226, 267.9787183723191, 268.76595318127625, 269.4958167039189, 270.16725070791483, 270.77928167814787, 271.3310222282221, 271.8216723870868, 272.2505207589181, 272.6169455545753, 272.9204154931351, 273.1604905721989, 273.33682270585314, 273.4491562293616, 273.4973282698545, 273.4973282698545, 273.4976768767981, 273.4977912421409, 273.497904040932, 273.49801489149615, 273.49812341874986, 273.49822925547153, 273.498332043543, 273.49843143516205, 273.49852709401887, 273.4986186964343, 273.4987059324545, 273.4987885069007, 273.4988661403669, 273.49893857016605, 273.4990055512187, 273.4990668568822, 273.49912227971754, 273.4991716321913, 273.4992147473105, 273.49925147918714, 273.4992817035321, 273.4993053180758, 273.4993222429142, 273.49933242077907, 273.49933581723144, 273.49933242077907, 273.49933242077907, 273.4359977358081, 273.2463022411139, 272.93117011407867, 272.4921366467481, 271.93134076604383, 271.25151461312544, 270.4559702326741, 269.54858343694355, 268.53377492319237, 267.4164887364914, 267.4164887364914, 264.8592185981979, 262.38903322735655, 260.09005182411045, 258.040563442823, 256.3103609522285, 254.95836432299495, 254.95836432299495, 253.94047487628086, 252.9284158081383, 251.92798410125806, 250.94491013785876, 249.98482487657634, 249.05322759884268, 248.15545440949995, 247.29664767207498, 246.48172655378664, 245.71535884900197, 245.00193424253223, 244.34553916591585, 243.74993339070932, 243.21852849285517, 242.75436831148465, 242.36011151408275, 242.03801636788126, 241.7899278047089, 241.61726685338886, 241.52102250021426, 241.50174602412415, 241.55954783902803, 241.69409686136464, 241.90462240651857, 242.1899186032323, 242.5483513007273, 242.5483513007273, 242.06828716612836, 242.19581957094863, 242.56556855706117, 243.16565008782175, 243.9767770490829, 244.97287915291258, 246.1219408580978, 246.1219408580978, 246.1664626288965, 246.29967949807133, 246.5205488629988, 246.8273421214326, 247.21765820015992, 247.6884423466546, 248.2360100366575, 248.85607581057468, 249.54378681300898, 250.2937607729333, 251.10012812725893, 251.95657795812508, 252.85640738438758, 253.7925740207521, 254.7577510939875, 255.74438478486024, 256.74475334701185, 257.7510275400937, 258.75533190418776, 259.749806395959, 260.726667904151, 261.6782711629842, 262.597168586727, 263.47616855715137, 264.3083917076965, 265.08732476383955, 265.8068715182998, 266.46140054212583, 267.0457892582633, 267.55546403266464, 267.9864359691777, 268.33533212806583, 268.59942192383505, 268.776638495767, 268.8655948839073, 268.8655948839073, 268.776638495767, 268.59942192383494, 268.33533212806583, 268.33533212806583, 268.2455132009304, 267.9768698336369, 267.5318349021202, 266.91443871163517, 266.13027249768936, 265.18643779094714, 264.09148210466935, 262.8553215271089, 261.48915091988044, 260.00534253555975, 260.00534253555975, 259.61058390883204, 259.0660442523364, 258.37437650854224, 257.5389504131713, 256.5638360781834, 255.4537841626004, 254.2142027277731, 252.85113088984974, 251.37120939780903, 249.78164828039894, 248.09019171960165, 246.30508032175658, 244.43501097015508, 242.48909445469974, 240.47681108505094, 238.4079645035115, 236.29263392266694, 234.141125020475, 231.963919732038, 229.77162518266948, 227.57492201104526, 225.38451233420574, 223.21106760791724, 223.21106760791724, 221.88902006081128, 220.60951067207395, 219.37548235430143, 218.18977341216964, 217.05511101424236, 215.97410492039523, 214.94924147928566, 213.98287790967015, 213.07723687872658, 212.23440138984768, 211.4563099916669, 210.74475231933457, 210.10136497829876, 209.52762778006183, 209.0248603385653, 208.59421903503608, 208.2366943582723, 207.9531086264875, 207.74411409595257, 207.61019146078502, 207.61019146078502, 207.628645228981, 207.68399288163857, 207.77619347306643, 207.90517879410345, 208.07085342257938, 208.27309479390715, 208.51175329175487, 208.7866523587311, 209.09758862700005, 209.44433206873123, 209.82662616627215, 210.24418810191776, 210.69670896713626, 211.18385399109684, 211.7052627883303, 212.26054962533874, 212.84930370595782, 213.47108947526, 214.12544694177393, 214.8118920177826, 215.5299168774461, 216.2789903324874, 217.05855822516025, 217.86804383820981, 218.70684832152358, 219.57435113515442, 220.46991050839108, 221.39286391453368, 222.3425285610258, 223.31820189457744, 224.31916212090778, 225.34466873872236, 226.39396308752896, 227.4662689088875, 228.56079292067977, 229.67672540397132, 230.81324080203512, 231.96949833109022, 233.14464260230648, 234.33780425461305, 235.54810059784427, 236.77463626574536, 238.0165038783565, 239.2727847132844, 240.54254938536462, 241.82485853421213, 243.11876351915203, 244.4233071210151, 245.73752425027953, 247.06044266103606, 248.39108367024608, 249.72846288176277, 251.07159091457834, 252.4194741347593, 253.7711153905276, 255.1255147499445, 256.4816702406513, 257.83857859111924, 259.1952359728606, 260.55063874305245, 261.90378418702164, 263.2536712600448, 264.5993013279118, 265.93967890570605, 267.27381239425563, 268.6007148137094, 269.9194045336951, 271.22890599952143, 272.52825045388397, 273.8164766535433, 275.0926315804438, 276.3557711467481, 277.60496089326455, 278.8392766807515, 280.05780537358714, 281.25964551529944, 282.44390799545477, 283.6097167074141, 284.75620919646883, 285.88253729787664, 286.98786776432655, 288.07138288236695, 289.1322810773436, 290.16977750639626, 291.1831046390794, 292.17151282517295, 293.1342708492682, 294.070666471713, 294.98000695552105, 295.86161957885224, 296.7148521326859, 297.5390734033193, 298.33367363933405, 299.09806500268417, 299.83168200357386, 300.53398191880126, 301.20444519326054, 301.8425758243038, 302.4479017286794, 303.0199750917749, 303.5583726989063, 304.0626962484082, 304.53257264629394, 304.9676542822674, 305.3676192868812, 305.7321717696535, 306.06104203796497, 306.35398679657453, 306.61078932760745, 306.83125965088107, 307.0152346644504, 307.1625782652693, 307.27318144987885, 307.34696239504626, 307.34696239504626, 307.3396891504948, 307.31787194506444, 307.28151836254824, 307.23064103967215, 307.16525766170264, 307.08539095629885, 306.9910686856126, 306.8823236366379, 306.759193609814, 306.6217214058861, 306.4699548110268, 306.303946580226, 306.123754418953, 305.92944096309714, 305.7210737571953, 305.4987252309538, 305.2624726740704, 305.0123982093685, 304.74858876425026, 304.4711360404809, 304.1801364823119, 303.8756912429567, 303.5579061494291, 303.2268916657572, 302.8827628545855, 302.5256393371785, 302.15564525183913, 301.77290921075826, 301.3775642553078, 300.96974780979474, 300.54960163369185, 300.1172717723613, 299.67290850628797, 299.21666629884135, 298.7487037425831, 298.2691835041393, 297.7782722676566, 297.2761406768617, 296.7629632757445, 296.2389184478858, 295.7041883544497, 295.1589588708638, 294.6034195222071, 294.03776341733067, 293.4621871817312, 292.8768908892033, 292.2820779922921, 291.6779552515725, 291.06473266377736, 290.44262338880174, 289.8118436756073, 289.17261278705246, 288.5251529236755, 287.8696891464565, 287.2064492985844, 286.5356639262577, 285.8575661985449, 285.1723918263335, 284.48037898039604, 283.7817682085995, 283.0768023522903, 282.36572646188085, 281.6487877116682, 280.9262353139155, 280.19832043222345, 279.4652960942251, 278.7274171036312, 277.9849399516594, 277.2381227278762, 276.4872250304834, 275.73250787608015, 274.97423360893197, 274.2126658097786, 273.44806920421127, 272.6807095706529, 271.9108536479714, 271.1387690427605, 270.3647241363169, 269.58898799135034, 268.8118302584552, 268.03352108237823, 267.2543310081153, 266.47453088686734, 265.6943917818918, 265.6943917818918, 265.47517518867267, 264.9324699704031, 264.07556196383393, 262.9191131025375, 261.4829105471588, 259.7915281216108, 257.87390584813454, 255.76285477549447, 253.4944955728373, 251.10764049502882, 248.64312929421135, 246.14313044032224, 243.65041960688268, 243.65041960688268, 241.4374348012375, 239.2294874870512, 237.02837849782327, 234.8359030896172, 232.653849476824, 230.48399737366867, 228.32811654265026, 226.18796535109743, 224.06528933701998, 221.9618197854213, 219.8792723162394, 217.8193454850615, 215.78371939775752, 213.77405434016293, 211.7919894239243, 209.83914124961746, 207.9171025882241, 206.0274410820458, 204.1716979661112, 202.35138681112392, 200.56799228897182, 198.82296896180753, 197.11774009568654, 195.45369649973128, 193.83219539176707, 192.25455929135563, 190.72207494112962, 189.2359922573064, 187.7975233102386, 186.40784133583247, 185.06807977863934, 183.77933136740222, 182.54264722381077, 181.3590360051909, 180.22946308182986, 179.15484974960617, 178.13607247856726, 178.13607247856726, 176.86643822612206, 175.6171335078298, 174.3887367468281, 173.1818166859531, 171.9969321244139, 170.83463165907057, 169.69545343043603, 168.57992487351936, 167.48856247362522, 166.42187152722295, 165.38034590799703, 164.36446783818496, 163.3747076653105, 162.41152364441416, 161.475361725884, 160.56665534898093, 159.68582524115948, 158.83327922327146, 158.00941202074733, 157.21460508083965, 156.44922639601413, 155.7136303335713, 155.00815747157503, 154.3331344411672, 153.6888737753377, 153.6888737753377, 152.66248890408735, 151.65481343700674, 150.66690428651026, 149.699797632875, 148.75450783743196, 147.83202637864082, 146.93332081216505, 146.05933375604093, 145.21098190200024, 144.38915505398853, 143.59471519488227, 142.82849558238843, 142.82849558238843, 142.23029951815755, 141.64276324495307, 141.06668926665134, 140.50286443101288, 139.95205885494357, 139.41502487260584, 138.89249600782153, 138.38518597216785, 137.89378769013024, 137.41897235265407, 136.96138850037653, 136.52166113779896, 136.10039087960592, 135.6981531302963, 135.31549729825298, 134.95294604531375, 134.61099457288032, 134.2901099455325, 133.99073045307318, 133.7132650118773, 133.45809260636304, 133.2255617713422, 133.01599011596545, 132.8296638899056, 132.66683759237435, 132.52773362450893, 132.52773362450893, 132.5114992352219, 132.46280547528272, 132.38168056300515, 132.26817151074215, 132.12234409764199, 131.94428283152865, 131.73409089992947, 131.49189011027687, 131.2178208193206, 130.9120418517901, 130.57473040835464, 130.20608196293455, 129.8063101494228, 129.37564663788334, 128.9143410002963, 128.42266056593036, 127.90089026642323, 127.34933247066266, 126.76830680956152, 126.15814999083011, 125.51921560385199, 124.85187391477689, 124.15651165194953, 123.43353178179802, 122.6833532753127, 121.90641086525, 121.1031547942024, 120.27405055368007, 119.419578614356, 118.54023414763037, 117.63652673867608, 116.70898009113097, 115.75813172360881, 114.78453265820383, 113.78874710117066, 112.77135211596323, 111.73293728882334, 110.67410438711204, 109.59546701058179, 108.49765023579232, 107.38129025387468, 106.24703400185513, 105.09553878775125, 103.92747190965827, 102.743510269046, 101.54433997849043, 100.33065596406766, 99.10316156264008, 97.86256811426887, 96.60959454998817, 95.34496697518034, 94.06941824879402, 92.78368755864778, 91.48851999306686, 90.18466610909996, 88.87288149756716, 87.55392634519097, 86.22856499406328, 84.89756549870515, 83.56169918097453, 82.22174018308101, 80.87846501896576, 79.53265212430733, 78.18508140541383, 76.83653378726251, 75.4877907609494, 74.13963393081065, 72.79284456147832, 71.4482031251332, 70.10648884921653, 68.76847926486312, 67.4349497563179, 66.10667311159602, 64.78441907464781, 63.46895389928801, 62.16103990514671, 60.861435035901366, 59.57089242004366, 58.29015993443733, 57.01997977091929, 55.76108800619522, 54.51421417527865, 53.28008084872158, 52.05940321388027, 50.852888660460565, 49.66123637058104, 48.48513691359307, 47.32527184589213, 46.182313315951774, 45.05692367480964, 45.05692367480964, 44.34015889095065, 43.648347524469045, 42.981906296663624, 42.34123664680993, 41.726724490349, 41.13873998642583, 40.57763731491934, 40.04375446309791, 39.53741302202832, 39.058917992861325, 38.608557603110455, 38.18660313303434, 37.79330875222786, 37.428911366520026, 37.09363047526995, 36.78766803914982, 36.51120835849031, 36.26441796226578, 36.04744550778253, 35.860421691134206, 35.703459168474694, 35.576652488158345, 35.48007803378778, 35.41379397820333, 35.37784024844137, 35.372238501684365, 35.396992112215116, 35.45208616938419, 35.53748748659177, 35.65314462127759, 35.79898790590846, 35.97492948994318, 36.18086339275044, 36.41666556744741, 36.68219397562143, 36.977288672887994, 37.30177190523591, 37.65544821609922, 38.038104564092976, 38.449510451341666, 38.88941806232219, 39.35756241313902, 39.85366151114016, 40.377416524779335, 40.92851196362085, 41.506615868379015, 42.11138001087852, 42.74244010381324, 43.39941602018013, 44.08191202225322, 44.08191202225322, 45.33634192285314, 46.636978814468364, 47.980831190568836, 49.36480814762518, 50.78572649428588, 52.240318072819775, 53.72523727598326, 55.23706874202401, 56.77233521012289, 58.327505518205044, 59.89900272472573, 61.483212335750636, 63.07649061840772, 63.07649061840772, 63.66331996396747, 64.7721467373052, 65.51797471030875, 66.38824953021805, 67.38008023160137, 68.49017205261276, 69.71483737986034, 71.05000799828645, 72.49124860536915, 72.49124860536915, 73.32011730200958, 74.13735049905176, 74.93147603380652, 75.69134612451046, 76.40629386074596, 77.06628294348099, 77.66204857270787, 78.18522750492667, 78.62847545475137, 78.98557019257788, 79.25149889104847, 79.42252849415951, 79.49625812118478, 79.49625812118478, 78.37661122542738, 77.09660139764947, 75.68350982504086, 74.16745410113231, 72.58074632157849, 70.95720440793764, 69.33143133738622, 67.73807764033688, 66.21110288454076, 64.78305188586017};
        double[] xDASegnoSegCanvB = new double[]{31.5, 298.5};
        double[] yDASegnoSegCanvB = new double[]{310.5, 31.5};
        double[] xDASegnoSegCanvC = new double[]{12.5, 14.065463221006098, 15.599058125255738, 17.06956514302078, 18.44704899201156, 19.703468073398085, 20.81324531789687, 21.753788861142993, 22.50595194889968, 23.05442270975948, 23.388035860690252, 23.5, 23.388035860690252, 23.05442270975948, 22.50595194889968, 21.753788861142993, 20.81324531789687, 19.7034680733982, 18.44704899201156, 17.06956514302078, 15.599058125255738, 14.065463221006212, 12.5, 10.934536778993902, 9.400941874744262, 7.930434856979218, 6.55295100798844, 5.296531926601915, 4.186754682103128, 3.2462111388570065, 2.4940480511003216, 1.9455772902405215, 1.6119641393097481, 1.5, 1.6119641393097481, 1.9455772902405215, 2.4940480511003216, 3.2462111388570065, 4.186754682103242, 5.296531926601915, 6.552951007988554, 7.930434856979332, 9.400941874744376, 10.934536778994016};
        double[] yDASegnoSegCanvC = new double[]{23.5, 23.388035860690252, 23.05442270975948, 22.50595194889968, 21.753788861142993, 20.81324531789687, 19.70346807339814, 18.44704899201156, 17.06956514302078, 15.599058125255738, 14.065463221006155, 12.5, 10.934536778993902, 9.400941874744262, 7.930434856979275, 6.55295100798844, 5.296531926601858, 4.186754682103185, 3.2462111388570065, 2.4940480511003216, 1.9455772902405215, 1.6119641393097481, 1.5, 1.6119641393097481, 1.9455772902405215, 2.4940480511002647, 3.2462111388570065, 4.186754682103185, 5.296531926601858, 6.55295100798844, 7.930434856979275, 9.40094187474432, 10.934536778993902, 12.500000000000057, 14.065463221006212, 15.599058125255794, 17.06956514302084, 18.447048992011673, 19.7034680733982, 20.81324531789693, 21.75378886114305, 22.505951948899735, 23.054422709759535, 23.388035860690252};
        double[] xDASegnoSegCanvD = new double[]{12.5, 14.065463221006098, 15.599058125255738, 17.06956514302078, 18.44704899201156, 19.703468073398085, 20.81324531789687, 21.753788861142993, 22.50595194889968, 23.05442270975948, 23.388035860690252, 23.5, 23.388035860690252, 23.05442270975948, 22.50595194889968, 21.753788861142993, 20.81324531789687, 19.7034680733982, 18.44704899201156, 17.06956514302078, 15.599058125255738, 14.065463221006212, 12.5, 10.934536778993902, 9.400941874744262, 7.930434856979218, 6.55295100798844, 5.296531926601915, 4.186754682103128, 3.2462111388570065, 2.4940480511003216, 1.9455772902405215, 1.6119641393097481, 1.5, 1.6119641393097481, 1.9455772902405215, 2.4940480511003216, 3.2462111388570065, 4.186754682103242, 5.296531926601915, 6.552951007988554, 7.930434856979332, 9.400941874744376, 10.934536778994016};
        double[] yDASegnoSegCanvD = new double[]{23.5, 23.388035860690252, 23.05442270975948, 22.50595194889968, 21.753788861142993, 20.81324531789687, 19.70346807339814, 18.44704899201156, 17.06956514302078, 15.599058125255738, 14.065463221006155, 12.5, 10.934536778993902, 9.400941874744262, 7.930434856979275, 6.55295100798844, 5.296531926601858, 4.186754682103185, 3.2462111388570065, 2.4940480511003216, 1.9455772902405215, 1.6119641393097481, 1.5, 1.6119641393097481, 1.9455772902405215, 2.4940480511002647, 3.2462111388570065, 4.186754682103185, 5.296531926601858, 6.55295100798844, 7.930434856979275, 9.40094187474432, 10.934536778993902, 12.500000000000057, 14.065463221006212, 15.599058125255794, 17.06956514302084, 18.447048992011673, 19.7034680733982, 20.81324531789693, 21.75378886114305, 22.505951948899735, 23.054422709759535, 23.388035860690252};

        //Transparent and mouse transparent rectangle surrounding group at base:
        Rectangle backgroundSegnoSeg = new Rectangle(0.0,0.0,330.0, 342.0);
        backgroundSegnoSeg.setFill(Color.TRANSPARENT);
        backgroundSegnoSeg.setStroke(Color.TRANSPARENT);
        backgroundSegnoSeg.setMouseTransparent(true);
        segnoSeg.getChildren().add(backgroundSegnoSeg);

        Canvas segnoSegCanv = new Canvas(289.59178082369203, 308.84696239504626);
        GraphicsContext grConSegnoSegCanv = segnoSegCanv.getGraphicsContext2D();
        grConSegnoSegCanv.setFill(Color.valueOf("0x000000ff"));
        grConSegnoSegCanv.setStroke(Color.valueOf("0x000000ff"));
        grConSegnoSegCanv.setLineWidth(1.0);
        grConSegnoSegCanv.setLineJoin(StrokeLineJoin.ROUND);
        grConSegnoSegCanv.fillPolygon(xDASegnoSegCanv, yDASegnoSegCanv, 1202);
        grConSegnoSegCanv.strokePolygon(xDASegnoSegCanv, yDASegnoSegCanv, 1202);
        segnoSegCanv.setTranslateX(27.508603062928046);
        segnoSegCanv.setTranslateY(10.50066418276856);
        Canvas segnoSegCanvB = new Canvas(330.0, 342.0);
        GraphicsContext grConSegnoSegCanvB = segnoSegCanvB.getGraphicsContext2D();
        grConSegnoSegCanvB.setStroke(Color.valueOf("0x000000ff"));
        grConSegnoSegCanvB.setLineWidth(21.0);
        grConSegnoSegCanvB.setLineJoin(StrokeLineJoin.ROUND);
        grConSegnoSegCanvB.strokePolyline(xDASegnoSegCanvB, yDASegnoSegCanvB, 2);
        segnoSegCanvB.setTranslateX(0.0);
        segnoSegCanvB.setTranslateY(0.0);
        Canvas segnoSegCanvC = new Canvas(25.0, 25.0);
        GraphicsContext grConSegnoSegCanvC = segnoSegCanvC.getGraphicsContext2D();
        grConSegnoSegCanvC.setFill(Color.valueOf("0x000000ff"));
        grConSegnoSegCanvC.setStroke(Color.valueOf("0x000000ff"));
        grConSegnoSegCanvC.setLineWidth(1.0);
        grConSegnoSegCanvC.setLineJoin(StrokeLineJoin.ROUND);
        grConSegnoSegCanvC.fillPolygon(xDASegnoSegCanvC, yDASegnoSegCanvC, 44);
        grConSegnoSegCanvC.strokePolygon(xDASegnoSegCanvC, yDASegnoSegCanvC, 44);
        segnoSegCanvC.setTranslateX(233.5);
        segnoSegCanvC.setTranslateY(120.5);
        Canvas segnoSegCanvD = new Canvas(25.0, 25.0);
        GraphicsContext grConSegnoSegCanvD = segnoSegCanvD.getGraphicsContext2D();
        grConSegnoSegCanvD.setFill(Color.valueOf("0x000000ff"));
        grConSegnoSegCanvD.setStroke(Color.valueOf("0x000000ff"));
        grConSegnoSegCanvD.setLineWidth(1.0);
        grConSegnoSegCanvD.setLineJoin(StrokeLineJoin.ROUND);
        grConSegnoSegCanvD.fillPolygon(xDASegnoSegCanvD, yDASegnoSegCanvD, 44);
        grConSegnoSegCanvD.strokePolygon(xDASegnoSegCanvD, yDASegnoSegCanvD, 44);
        segnoSegCanvD.setTranslateX(66.5);
        segnoSegCanvD.setTranslateY(189.5);        
        
        segnoSeg.getChildren().addAll(segnoSegCanv, segnoSegCanvB, segnoSegCanvC, segnoSegCanvD);        
        segnoSeg.setTranslateX(852.5);
        segnoSeg.setTranslateY(-36.5);
        segnoSeg.setScaleX(0.06);
        segnoSeg.setScaleY(0.06);

        segnoSeg.setOnMousePressed((mP)->{
            double dFromCenter;
            dFromCenter = mP.getX() - (segnoSeg.getBoundsInLocal().getWidth()/2.0);
            segnoSeg.setTranslateX(segnoSeg.getTranslateX() + dFromCenter);
            dFromCenter = mP.getY() - (segnoSeg.getBoundsInLocal().getHeight()/2.0);
            segnoSeg.setTranslateY(segnoSeg.getTranslateY() + dFromCenter);            
        });
        segnoSeg.setOnMouseDragged((mD)->{
            double dFromCenter;
            dFromCenter = mD.getX() - (segnoSeg.getBoundsInLocal().getWidth()/2.0);
            segnoSeg.setTranslateX(segnoSeg.getTranslateX() + dFromCenter);
            dFromCenter = mD.getY() - (segnoSeg.getBoundsInLocal().getHeight()/2.0);
            segnoSeg.setTranslateY(segnoSeg.getTranslateY() + dFromCenter);            
        });
        segnoSeg.setOnMouseReleased((mR)->{
            System.out.println("Position group: 'segnoSeg.setTranslateX(" + segnoSeg.getTranslateX() +
            ");' and  'segnoSeg.setTranslateY(" + segnoSeg.getTranslateY() + ");.");
        });

        //End of imported canvii and functions.
        
    }
    
    private void initPapColorBox(){
        Color initCol;
        
        papColBox.setTranslateX(clearButton.getBoundsInParent().getMinX() - 160.0);
        papColBox.setTranslateY(sPiecePane.getBoundsInParent().getMinY() -
                papColBox.getBoundsInLocal().getHeight() - 5.0);
        papColBox.setLabelText("Slightly darker than paper");
        SPTooltip pColBoxDescr = new SPTooltip("Full-width areas of at least this brightness\n" +
                "will be marked for deletion (in the third step).");
        Tooltip.install(papColBox, pColBoxDescr); 
        
        initCol = papColBox.getColorThreshold();
        sPiecePane.setStyle("-fx-color-paper: rgba(" + initCol.getRed()*255 +
                        "," + initCol.getGreen()*255 + "," + initCol.getBlue()*255 + 
                "," + initCol.getOpacity() +");");
        papColBox.colProperty().addListener(new ChangeListener<Color>(){
            @Override
            public void changed(ObservableValue<? extends Color> observable, Color oldValue, Color newPapCol) {                
                sPiecePane.setStyle("-fx-color-paper: rgba(" + newPapCol.getRed()*255 +
                        "," + newPapCol.getGreen()*255 + "," + newPapCol.getBlue()*255 + "," + 
                        newPapCol.getOpacity() + ");");
            }            
        });
    }
    
    private void saveMarch(){
        if(sPContImage == null){
            storeShrunkPieceGraphics();
        }
        if(sPContImage != null){
            PageOrientation prefPageOr;
            Printer defPrinter;
            PrintSides prefSides, minSides;
            double cW, cH, cumWidth, cumHeight, addLength,
                    pPInch = 72.0, availableSize,
                    selW, selH;
            int nPages, fP, lP;
            Color cColor;
            ArrayList<double[]> marches;
            ArrayList<Group> pages = new ArrayList<Group>();
            Group pageMarches;
            Canvas pSelCanvas;
            PageLayout setLayout;
            
            cumWidth = 0.0; cumHeight = 0.0;
            availableSize = 0.0;
            nPages = 0; addLength = 0.0;
            
            //Markup printable Node:
            cW = sPContImage.getWidth();
            cH = sPContImage.getHeight();
            for(int yIndT = 0; yIndT < cH; yIndT++){
                for(int xIndT = 0; xIndT < cW; xIndT++){
                    cColor = pReader.getColor(xIndT, yIndT);
                    if(cColor.equals(Color.TRANSPARENT)){
                        pWriter.setColor(xIndT, yIndT, Color.WHITE);
                    }
                }
            }
            storeShrunkPieceGraphics();
            
            if(pPr != null){
                if(pPr.isShowing()){
                    pPr.close();
                }
            }
            pPr = new PrintSelector();
            pPr.setContent(sPContImage, boundsMBounds);
            pPr.setX(AWIDTH*0.5);
            pPr.setY(AHEIGHT*0.25);
            pPr.show();
            
            defPrinter = Printer.getDefaultPrinter();
            prefSides = null; minSides = null;
            for(PrintSides pS : defPrinter.getPrinterAttributes().getSupportedPrintSides()){
                minSides = pS;
                if(pS.equals(PrintSides.ONE_SIDED)){
                    prefSides = pS;
                }
            }
            if(prefSides == null){
                prefSides = minSides;
            }
            
            marches = pPr.getPrintSelections();
            for(double[] mBs : marches){
                if(mBs[2] > cumWidth){
                    cumWidth = mBs[2];
                }
                cumHeight += mBs[3] - mBs[1];
            }
            if(cumHeight > cumWidth){
                prefPageOr = PageOrientation.PORTRAIT;    
            } else {
                prefPageOr = PageOrientation.LANDSCAPE;
            }            
            PrinterJob prMarch = PrinterJob.createPrinterJob();
            Paper pap = defPrinter.getPrinterAttributes().getDefaultPaper();
            PageLayout defPagLayout = defPrinter.createPageLayout(pap, prefPageOr, 
                    Printer.MarginType.HARDWARE_MINIMUM);
            cumHeight = 0.0;
            cumWidth = 0.0;
            nPages = 1;
            pageMarches = new Group();
            switch(defPagLayout.getPageOrientation()){
                case PORTRAIT:
                    availableSize = Screen.getPrimary().getDpi()*
                            (defPagLayout.getPrintableHeight()/pPInch);
                    for(double[] mBs : marches){
                        selH = (mBs[3]-mBs[1]);                        
                        addLength = selH;
                        if ((cumHeight + addLength)> availableSize){
                            nPages++;
                            cumHeight = addLength;
                        } else {
                            cumHeight += addLength;
                        }                        
                    }
                    break;
                case LANDSCAPE:
                    availableSize = Screen.getPrimary().getDpi()*
                            defPagLayout.getPrintableWidth()/pPInch;
                    for(double[] mBs : marches){                        
                        selW = (mBs[2]-mBs[0]);                        
                        addLength = selW;
                        if ((cumWidth + addLength)> availableSize){
                            nPages++;
                            cumWidth = addLength;
                        } else {
                            cumWidth += addLength;
                        }                        
                    }
                    break;
            }
            prMarch.getJobSettings().setPageLayout(defPagLayout);
            prMarch.getJobSettings().setPrintSides(prefSides);
            prMarch.getJobSettings().setPageRanges(new PageRange(1,nPages));
            if(prMarch.showPageSetupDialog(stageShrinker.getOwner())){
                if(pPr.isShowing()){
                    pPr.disableScrollPaneContent();
                }
                setLayout = prMarch.getJobSettings().getPageLayout();
                //After setting page layout:
                nPages = 1;
                pages.clear();
                cumWidth = 0.0;
                cumHeight = 0.0;
                marches = pPr.getPrintSelections();
                if(marches.size() > 0){
                    switch(setLayout.getPageOrientation()){
                        case PORTRAIT:
                            availableSize = Screen.getPrimary().getDpi()*
                                    (setLayout.getPrintableHeight()/pPInch);
                            for(double[] mBs : marches){
                                selW = (mBs[2]-mBs[0]);
                                selH = (mBs[3]-mBs[1]);
                                pSelCanvas = new Canvas();                            
                                drawSelectionToCanvas(pSelCanvas, mBs[0], mBs[1], selW, selH);
                                addLength = selH;
                                if ((cumHeight + addLength)> availableSize){
                                    nPages++;
                                    pages.add(pageMarches);
                                    pageMarches = new Group();
                                    cumHeight = 0.0;
                                }   
                                pSelCanvas.setTranslateY(cumHeight);
                                pageMarches.getChildren().add(pSelCanvas);
                                cumHeight += addLength;
                            }
                            if(pageMarches.getChildren().size() > 0){
                                pages.add(pageMarches);
                            }
                            break;
                        case LANDSCAPE:
                            availableSize = Screen.getPrimary().getDpi()*
                                    setLayout.getPrintableWidth()/pPInch;
                            for(double[] mBs : marches){
                                selW = (mBs[2]-mBs[0]);
                                selH = (mBs[3]-mBs[1]);
                                pSelCanvas = new Canvas();
                                drawSelectionToCanvas(pSelCanvas, mBs[0], mBs[1], selW, selH);                       
                                addLength = selW;
                                if ((cumWidth + addLength)> availableSize){
                                    nPages++;
                                    pages.add(pageMarches);
                                    pageMarches = new Group();
                                    cumWidth = 0.0;
                                }        
                                pSelCanvas.setTranslateX(cumWidth);
                                pageMarches.getChildren().add(pSelCanvas);
                                cumWidth += addLength;
                            }
                            if(pageMarches.getChildren().size() > 0){
                                pages.add(pageMarches);
                            }
                            break;
                    }
                    fP = 1;
                    lP = nPages;
                    prMarch.getJobSettings().setPageRanges(new PageRange(fP, lP));
                    if(prMarch.showPrintDialog(stageShrinker.getOwner())){
                        Printer chosenPrinter = prMarch.getPrinter();
                        prMarch.getJobSettings().setPageLayout(chosenPrinter.createPageLayout(
                                prMarch.getJobSettings().getPageLayout().getPaper(), 
                                prMarch.getJobSettings().getPageLayout().getPageOrientation(),
                                setLayout.getLeftMargin(),setLayout.getRightMargin(),
                                setLayout.getTopMargin(), setLayout.getBottomMargin()));                        
                        if(pPr.isShowing()){
                            pPr.close();
                        }
                        PageRange[] pRToPRint = prMarch.getJobSettings().getPageRanges();
                        if(pRToPRint != null){
                            for(int pRangeIndex = 0; pRangeIndex < pRToPRint.length; pRangeIndex++){
                                fP = pRToPRint[pRangeIndex].getStartPage();
                                lP = pRToPRint[pRangeIndex].getEndPage();
                                addRangeToPrint(prMarch, fP, lP, pages);
                            }
                        } else {
                            addRangeToPrint(prMarch, fP, lP, pages);
                        }                    
                        prMarch.endJob();
                    } else {
                        if(pPr.isShowing()){
                            pPr.close();
                        }
                    }
                } else {
                    if(pPr.isShowing()){
                        pPr.close();
                    }
                }
            } else {
                if(pPr.isShowing()){
                    pPr.close();
                }
            }
        }
    }    
    
    private void drawSelectionToCanvas(Canvas toDrawTo, double minX, double minY, 
            double  width, double height){
        double mX, mY;
        if(sPContImage != null){
            mX = sPContImage.getWidth();
            if(minX < mX){
                mX = minX;
            }
            if((mX + width)>sPContImage.getWidth()){
                width = sPContImage.getWidth() - mX;
            }
            mY = sPContImage.getHeight();
            if(minY < mY){
                mY = minY;
            }
            if((mY + height)>sPContImage.getHeight()){
                height = sPContImage.getHeight() - mY;
            }
            toDrawTo.setWidth(width);
            toDrawTo.setHeight(height);
            toDrawTo.getGraphicsContext2D().drawImage(sPContImage, mX,mY,
                    width, height, 0.0, 0.0, width, height);            
            toDrawTo.getGraphicsContext2D().setLineDashes(1.0,1.0);
            toDrawTo.getGraphicsContext2D().strokeRect(0.0, 0.0, width*2.0, height);
        }
    }
    
    private void addRangeToPrint(PrinterJob fromJob, int firstPageIndex,
            int lastPageIndex, ArrayList<Group> virtualPages){
        Group scaledPage;
        
        firstPageIndex -= 1;
        lastPageIndex -= 1;
        
        for(int pInd = firstPageIndex; pInd <= lastPageIndex; pInd++){ 
            if((pInd > -1) && (pInd < virtualPages.size())){ 
                scaledPage = virtualPages.get(pInd);
                scaledPage.setScaleX(DOTS_PER_POINT);                
                scaledPage.setScaleY(DOTS_PER_POINT);
                scaledPage.setTranslateX(0.0-scaledPage.getBoundsInParent().getMinX());
                scaledPage.setTranslateY(0.0-scaledPage.getBoundsInParent().getMinY());
                fromJob.printPage(scaledPage);  
            }
        }
    }

    private void generateSPGraphicsRecovery(){
        if(shrunkPiece != null){
            preModificationImage = new WritableImage((int)shrunkPiece.getWidth(),
                        (int)shrunkPiece.getHeight());
            shrunkPiece.snapshot(null, preModificationImage);
        }
    }
    
    /*  Actions {ENTER_TEXT, ESCAPE_TEXT_ENTRY, ESCAPE_SEGNOSEG, ESCAPE_REPS,
            ESCAPE_REP_LR, UNGROUP_REP_VIEWS, REP_SIGN_MERGE, IMAGE_IMPORT, PIECE_CROP,
            ESCAPE_SHRINK, SHRINK, DELETE_SHRINK_AREA, ESCAPE_REFACTORING, COMPLETE_REF_SEL, 
            DELETE_SELECTED_AREA, SCALE_REF_AREA, SETTLE_REF_AREA, SAVE_MARCH, SEGNO_MERGE,
            ADD_REF_AREA, SHRUNK_PIECE_PREPEND};
    */
    
    private void handleUndoRequest(){
        int sPerfActions;
        Actions lastAction, actionBeforeLast;
        
        lastAction = null;
        actionBeforeLast = null;
        
        sPerfActions = actionsPerformed.size();
        if(sPerfActions > 0){
            lastAction = actionsPerformed.get(sPerfActions - 1);
            if(sPerfActions > 1){
                actionBeforeLast = actionsPerformed.get(sPerfActions - 2);
            }    
            if(lastAction.equals(Actions.REPS_SET_VISIBLE)){
                escapeRepPlacement();
                if(sPerfActions > 0){
                    lastAction = actionsPerformed.get(sPerfActions - 1);
                }
                if(sPerfActions > 1){
                    actionBeforeLast = actionsPerformed.get(sPerfActions - 2);
                } 
            }
            switch(lastAction){
                
                case REP_SIGN_MERGE:
                case SEGNO_MERGE:
                case ENTER_TEXT:
                    recoverShrunkPieceGraphics();                                 
                    actionsPerformed.clear();
                    break;
                case ESCAPE_TEXT_ENTRY:
                case ESCAPE_SEGNOSEG:
                case ESCAPE_REPS:
                case ESCAPE_REP_LR:
                case UNGROUP_REP_VIEWS:
                case ESCAPE_REFACTORING:
                    break;
                case IMAGE_IMPORT:                                       
                    actionsPerformed.clear();
                    undoImportClipboardImage(); 
                    break;
                case PIECE_CROP:                    
                    discardClearAreas();                                 
                    actionsPerformed.clear();
                    undoCropImage();       
                    break;
                case ESCAPE_SHRINK:
                    setClearAreas();
                    break;
                case SHRINK:                    
                    recoverShrunkPieceGraphics();
                    actionsPerformed.clear();
                    setClearAreas();                    
                    break;
                case DELETE_SHRINK_AREA:
                    restoreDeletedClearArea();
                    break;
                case SCALE_REF_AREA:
                    recoverShrunkPieceGraphics();
                    resetTransformationSelection();
                    actionsPerformed.clear();
                case ADD_REF_AREA:
                case COMPLETE_REF_SEL:
                    resetTransformationSelection();
                    break;
                case DELETE_SELECTED_AREA:
                    discardClearAreas();
                    recoverShrunkPieceGraphics();
                    actionsPerformed.clear();
                    break;
                case SETTLE_REF_AREA:
                    discardClearAreas();
                    recoverShrunkPieceGraphics();
                    actionsPerformed.clear();
                    break;                
                case SAVE_MARCH:
                    saveMarch();
                    break;     
                case SHRUNK_PIECE_PREPEND:
                    undoShrunkPiecePrependance();
                    actionsPerformed.clear();
                    break;
            }
            if(actionsPerformed.size()>0){
                actionsPerformed.remove(actionsPerformed.size() - 1);
            }
        }
    }
    
    private void recoverShrunkPieceGraphics(){
        if(preModificationImage != null){
            double w, h;
            
            w = preModificationImage.getWidth();
            h = preModificationImage.getHeight();
            
            clearShrunkPieceContext();
            shrunkPiece.setWidth(w);
            shrunkPiece.setHeight(h);
            sPCont.drawImage(preModificationImage, 0.0, 0.0);
            storeShrunkPieceGraphics();
        }
    }
    
    private void storeShrunkPieceGraphics(){
        if(shrunkPiece != null){
            sPContImage = new WritableImage((int)shrunkPiece.getWidth(),
                        (int)shrunkPiece.getHeight());
            shrunkPiece.snapshot(null, sPContImage);
            pReader = sPContImage.getPixelReader();
        }
    }
}
