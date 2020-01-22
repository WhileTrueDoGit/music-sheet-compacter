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

import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 *
 * @author P.C.T. Kolkman
 */
public abstract class ResizableArea extends Group {
    
    private Rectangle boundsResizeArea, intResizeAreaBounds;
    private Node resizeNode;
    private Bounds offsetClip;
    private Group myGuideRects;
    private double wVerGuide, hHorGuide, sTXAB, sTYAB, mXS, mYS, wInitAB, hInitAB,
            sXGuestNode, sYGuestNode;
    private boolean vertRestriction, horRestriction;
    
    public ResizableArea(){
        this(null);
    }
    
    public ResizableArea(Node toResize){
        resizeNode = toResize;
        
        myGuideRects = new Group();
        intResizeAreaBounds = new Rectangle();
        boundsResizeArea = new Rectangle();
        
        wInitAB = Double.NaN;
        hInitAB = Double.NaN;
        
        if(getChildren().indexOf(boundsResizeArea) < 0){
            getChildren().add(0,boundsResizeArea);
        }
        boundsResizeArea.setMouseTransparent(true);        
        boundsResizeArea.boundsInParentProperty().addListener((ObservableValue<? extends Bounds> bsCA,
                Bounds oBsCA, Bounds nBsCA) -> {
            double distInit;
            
            for(Node g : myGuideRects.getChildren()){
                ((GuideRectangle)g).takePosition();
            }
            intResizeAreaBounds.setWidth(nBsCA.getWidth() + (wVerGuide*2.0));
            intResizeAreaBounds.setHeight(nBsCA.getHeight() + (hHorGuide*2.0));
            intResizeAreaBounds.setTranslateX(nBsCA.getMinX() - wVerGuide);
            intResizeAreaBounds.setTranslateY(nBsCA.getMinY() - hHorGuide);
            
            if(resizeNode != null){
                if(nBsCA.getWidth() != oBsCA.getWidth()){
                    if(!Double.isNaN(wInitAB)){
                        distInit = resizeNode.getBoundsInLocal().getWidth()*
                                resizeNode.getScaleX()*0.5;
                        resizeNode.setScaleX(nBsCA.getWidth()/wInitAB);                       
                        resizeNode.setTranslateX(resizeNode.getTranslateX()+
                            resizeNode.getBoundsInLocal().getWidth()*
                                resizeNode.getScaleX()*0.5 - distInit);
                    }
                }
                if(nBsCA.getHeight() != oBsCA.getHeight()){
                    if(!Double.isNaN(hInitAB)){
                        distInit = resizeNode.getBoundsInLocal().getHeight()*
                                resizeNode.getScaleY()*0.5;
                        resizeNode.setScaleY(nBsCA.getHeight()/hInitAB);                       
                        resizeNode.setTranslateY(resizeNode.getTranslateY()+
                            resizeNode.getBoundsInLocal().getHeight()*
                                resizeNode.getScaleY()*0.5 - distInit);
                    }
                }
                if(nBsCA.getMinX() != oBsCA.getMinX()){
                    resizeNode.setTranslateX(resizeNode.getTranslateX() + nBsCA.getMinX() -
                            oBsCA.getMinX());
                }
                if(nBsCA.getMinY() != oBsCA.getMinY()){
                    resizeNode.setTranslateY(resizeNode.getTranslateY() + nBsCA.getMinY() -
                            oBsCA.getMinY());
                }
            }
        }); 
        
        sTXAB = 0.0; sTYAB = 0.0; mXS = 0.0; mYS = 0.0;
        sXGuestNode = 0.0; sYGuestNode = 0.0;
        hHorGuide = 0.0;
        wVerGuide = 0.0;
        
        intResizeAreaBounds.setFill(Color.TRANSPARENT);
        intResizeAreaBounds.setStroke(Color.TRANSPARENT);
        intResizeAreaBounds.setStrokeWidth(1.0);        
        intResizeAreaBounds.setOnMouseEntered((mE)->{            
            myGuideRects.setVisible(true);
            setOnMouseEnteredArea();
            setCursor(Cursor.MOVE);
        });
        intResizeAreaBounds.setOnMouseExited((mEx)->{
            Bounds mBounds = intResizeAreaBounds.getBoundsInLocal();
            if ((mEx.getX() < mBounds.getMinX()) || (mEx.getY() < mBounds.getMinY()) ||
                    (mEx.getX() > mBounds.getMaxX()) ||
                    (mEx.getY() > mBounds.getMaxY())){
                myGuideRects.setVisible(false);
                setOnMouseExitedArea();
            }
            setCursor(null);
        });
        intResizeAreaBounds.setOnMousePressed((mP)->{            
            sTXAB = boundsResizeArea.getTranslateX();
            sTYAB = boundsResizeArea.getTranslateY();
            mXS = mP.getScreenX();
            mYS = mP.getScreenY();
        });
        intResizeAreaBounds.setOnMouseDragged((mD)->{
            double dPix;
            if(!horRestriction){
                dPix = mXS - mD.getScreenX();
                boundsResizeArea.setTranslateX(sTXAB - dPix);
            }
            if(!vertRestriction){
                dPix = mYS - mD.getScreenY();
                boundsResizeArea.setTranslateY(sTYAB - dPix);
            }
        });
        
        myGuideRects.setVisible(false);
        myGuideRects.getChildren().addListener((ListChangeListener.Change<? extends Node> c) -> {
            if(c.next()){
                if(c.wasAdded()){
                    for(int addInd = 0; addInd < c.getAddedSize(); addInd++){
                        if(c.getAddedSubList().get(addInd).getClass().getSimpleName().equals("GuideRectangle")){
                            GuideRectangle gR = ((GuideRectangle)c.getAddedSubList().get(addInd));
                            switch(gR.getGuideOrientation()){
                                case TOP:
                                case BOTTOM:
                                    hHorGuide = gR.getHeightGuide() + 2.0;
                                    break;
                                case LEFT:
                                case RIGHT:
                                    wVerGuide = gR.getWidthGuide() + 2.0;
                                    break;
                            }
                        }
                    }
                }
            }
        }); 
        
        getChildren().addAll(intResizeAreaBounds, myGuideRects);
    }  
    
    public abstract void setOnMouseEnteredArea();
    public abstract void setOnMouseExitedArea();
    public abstract void setGuides();   

    public void setHorizontalRestriction(boolean restrictHorizontalAdjustments){
        horRestriction = restrictHorizontalAdjustments;
    }
    
    public void setVerticalRestriction(boolean restrictVerticalAdjustments){
        vertRestriction = restrictVerticalAdjustments;
    }
    
    public Group getGuideGroup(){
        return myGuideRects;
    }
    
    public Rectangle getAreaBounds(){
        return boundsResizeArea;
    }
    
    public void setResizableNode(Node toResizeAlong){
        resizeNode = toResizeAlong;
    }
    
    public void setTranslateXRA(double newTX){
        boundsResizeArea.setTranslateX(newTX);
    }
    public void setTranslateYRA(double newTY){
        boundsResizeArea.setTranslateY(newTY);
    }
    public void setWidthExclusive(double newWidth){
        if(Double.isNaN(wInitAB)){
            wInitAB = newWidth;
        }            
        boundsResizeArea.setWidth(newWidth);
    }
    public void setHeightExclusive(double newHeight){
        if(Double.isNaN(hInitAB)){
            hInitAB = newHeight;
        }
        boundsResizeArea.setHeight(newHeight);
    }    
    
    public double getTranslateXRA(){
        return boundsResizeArea.getTranslateX();
    }
    public double getTranslateYRA(){
        return boundsResizeArea.getTranslateY();
    }
    public double getWidthExclusive(){
        return boundsResizeArea.getBoundsInLocal().getWidth() - (2.0*boundsResizeArea.getStrokeWidth());
    }
    public double getHeightExclusive(){
        return boundsResizeArea.getBoundsInLocal().getHeight() - (2.0*boundsResizeArea.getStrokeWidth());
    }
    
    
}
