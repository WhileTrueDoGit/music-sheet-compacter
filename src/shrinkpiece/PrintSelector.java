/*
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

import java.util.ArrayList;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

/**
 *
 * @author P.C.T. Kolkman
 */
public class PrintSelector extends Stage {
    
    private double sW, sH, cW, cH;
    private Scene previewScene;
    private ScrollPane scrollPrev;
    private Group scPaneContent;
    private ImageView contView;
    private Bounds mBounds;
    private Button cMenuButton;
    private ContextMenu cMenu;
    private ArrayList<PrintSelection> marchSelections;
    private static PrintSelection selectedPSel;
    
    public PrintSelector(){
        scrollPrev = new ScrollPane();     
        previewScene = new Scene(scrollPrev);
        contView = new ImageView();
        scPaneContent = new Group();
        marchSelections = new ArrayList<PrintSelection>();
        cMenuButton = new Button();
        cMenu = new ContextMenu();
        selectedPSel = null;
        
        sW = 0.0; sH = 0.0;
        cW = 0.0; cH = 0.0;        
        contView.setFitWidth(0.0);
        contView.setFitHeight(0.0);
        mBounds = null;
        
        setMaxWidth(ShrinkPiece.AWIDTH);
        setMaxHeight(ShrinkPiece.AHEIGHT);        
        
        widthProperty().addListener(new ChangeListener<Number>(){
            @Override
            public void changed(ObservableValue<? extends Number> observable, 
                    Number oldValue, Number newValue) {
                sW = (double) newValue;
                if(Double.isNaN((double) oldValue)){
                    oldValue = newValue;
                }
                sizeChangeStage(((double)newValue/(double)oldValue),1.0);
            }   
        });
        heightProperty().addListener(new ChangeListener<Number>(){
            @Override
            public void changed(ObservableValue<? extends Number> observable, 
                    Number oldValue, Number newValue) {
                sH = (double) newValue;
                if(Double.isNaN((double) oldValue)){
                    oldValue = newValue;
                }
                sizeChangeStage(1.0, ((double)newValue/(double)oldValue));
            }
        });
        setWidth(ShrinkPiece.AWIDTH*0.25);
        setHeight(ShrinkPiece.AHEIGHT*0.25);

        cMenuButton.setVisible(false);
        cMenuButton.setContextMenu(cMenu);
        MenuItem delSel = new MenuItem("Verwijder printselectie");
        delSel.setOnAction((ac)->{ 
            marchSelections.remove(selectedPSel);      
            scPaneContent.getChildren().remove(selectedPSel);
        });
        cMenu.getItems().add(delSel);
        
        scPaneContent.getChildren().add(cMenuButton);    
        
        adjustScrollPaneSize();
        scrollPrev.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPrev.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPrev.setPannable(false);
        scrollPrev.setContent(scPaneContent);
        
        setScene(previewScene);
        setTitle("Printselectie");
    }
    
    public void setContent(Image printContent, Bounds marchBounds){
        double lX, lY, sX, sY;
        scPaneContent.getChildren().clear();
        contView.setImage(printContent);
        scPaneContent.getChildren().add(contView);
        cW = printContent.getWidth();
        cH = printContent.getHeight(); 
        mBounds = marchBounds;
        sX = sW/ShrinkPiece.AWIDTH;
        contView.setFitWidth(cW*sX);
        sY = sH/ShrinkPiece.AHEIGHT;
        contView.setFitHeight(cH*sY);
        addMarchSelections();
    }
    
    public void showPages(ArrayList<Group> printPages){
        double w;
        Group pagesAside;
        
        if(printPages.size()>0){
            w = 0.0;
            pagesAside = new Group();

            for(Group p : printPages){
                p.setTranslateX(w);
                p.setTranslateY(0.0);
                w += p.getBoundsInLocal().getWidth();
                pagesAside.getChildren().add(p);
            }

            pagesAside.setScaleX(0.5);
            pagesAside.setTranslateX(0.0-pagesAside.getBoundsInParent().getWidth()*0.5);
            pagesAside.setScaleY(0.5);
            pagesAside.setTranslateY(0.0-pagesAside.getBoundsInParent().getHeight()*0.5);
            
            setWidth((pagesAside.getBoundsInParent().getWidth()/printPages.size())*1.1);
            setHeight(pagesAside.getBoundsInParent().getHeight()*1.1);
            
            scrollPrev.setContent(pagesAside);
        }
    }
    
    public void disableScrollPaneContent(){
        scPaneContent.setDisable(true);
    }
    
    private void sizeChangeStage(double xScale, double yScale){       
        adjustScrollPaneSize();        

        contView.setFitWidth(contView.getFitWidth()*xScale);
        contView.setFitHeight(contView.getFitHeight()*yScale);

        updateMarchSelectionSizes(xScale, yScale);
    }
    
    private void adjustScrollPaneSize(){
        scrollPrev.setPrefWidth(sW);
        scrollPrev.setPrefHeight(sH);
    }
    
    private void addMarchSelections(){
        double sX, sY, cumW, cumH, wB, hB, shortest, largest;
        int nMs;
        
        sX = contView.getFitWidth()/cW;
        sY = contView.getFitHeight()/cH;
        
        cumW = 0.0; cumH = 0.0;
        wB = mBounds.getWidth()*sX;
        hB = mBounds.getHeight()*sY;
        
        shortest = mBounds.getHeight();
        nMs = ((int)(cH/mBounds.getHeight()))+1;
        for(int mInd = 0; mInd < nMs; mInd++){ 
            if(cumH < cH){
                PrintSelection bMarch = new PrintSelection(0.0, cumH*sY,wB,hB,
                    contView.getFitWidth() - 1.0, 
                    contView.getFitHeight() - 1.0, cMenu);                
                cumH += mBounds.getHeight();
                marchSelections.add(bMarch);
//                bMarch.setOnMousePressed((mP)->{
//                    if(mP.getButton().equals(MouseButton.SECONDARY)){
//                        showContextMenu(bMarch, mP.getScreenX(), mP.getScreenY());
//                    }
//                });
                bMarch.setOnMouseReleased((mR)->{
                    if(!(bMarch.getDragged() ||
                            (bMarch.getContextMenuRequested()))){
                        bMarch.flipSides();
                    }
                    updateMarchBPositions(bMarch);
                });                
            } else {
                break;
            }
        }
        if(marchSelections.size() > 0){
            scPaneContent.getChildren().removeAll(marchSelections);
            scPaneContent.getChildren().addAll(marchSelections);
        }
        
    }
    
    private void updateMarchBPositions(PrintSelection startingFromHere){
        double yRed;
        int bInd;
        Bounds mBParent;
        
        bInd = marchSelections.indexOf(startingFromHere);
        if(bInd > -1){
            mBParent = marchSelections.get(bInd).getBoundsInParent();
            if(bInd > 0){
                if(marchSelections.get(bInd - 1).getBoundsInParent().getMaxY() >
                        startingFromHere.getTranslateY()){
                    startingFromHere.setTranslateY(
                            marchSelections.get(bInd - 1).getBoundsInParent().getMaxY() +
                            1.0);
                    mBParent = startingFromHere.getBoundsInParent();
                }
            } 
            if(bInd < (marchSelections.size() - 1)){                
                yRed = marchSelections.get(bInd+1).getTranslateY()-
                       mBParent.getMaxY(); 
                for(int mSInd = (bInd+1); mSInd < marchSelections.size(); mSInd++){
                    marchSelections.get(mSInd).setTranslateY(
                        marchSelections.get(mSInd).getTranslateY() - yRed);
                }
            }
        }
    }
    
    private void updateMarchSelectionSizes(double scaleX, double scaleY){
        if(marchSelections.size()>0){
            for(PrintSelection pS : marchSelections){
                pS.setTranslateX(pS.getTranslateX()*scaleX);
                pS.setTranslateY(pS.getTranslateY()*scaleY);
                pS.setWidth(pS.getWidth()*scaleX);
                pS.setHeight((pS.getHeight())*scaleY);                
            }
        }
    }
    
    private void showContextMenu(PrintSelection forSelection, double atScreenX, double atScreenY){
        selectedPSel = forSelection;
        cMenu.show(selectedPSel, atScreenX, atScreenY);
    }
    
    public static void setSelectedPrintSelection(PrintSelection selected){
        selectedPSel = selected;
    }
    
    public ArrayList<double[]> getPrintSelections(){
        ArrayList<double[]> sels;
        
        sels = new ArrayList<double[]>();
        
        if(marchSelections.size()>0){            
            double sX, sY;
            double[] bndsSel;
            Bounds pSBounds;

            sX = contView.getFitWidth()/cW;
            sY = contView.getFitHeight()/cH;
            
            for(PrintSelection pS : marchSelections){
                bndsSel = new double[4];
                bndsSel[0] = pS.getTranslateX()/sX;
                bndsSel[1] = pS.getTranslateY()/sY;
                bndsSel[2] = (pS.getTranslateX() + pS.getWidth())/sX;
                bndsSel[3] = (pS.getTranslateY() + pS.getHeight())/sY;
                sels.add(bndsSel);                
            }
        }
        
        return sels;
    }
    
}
