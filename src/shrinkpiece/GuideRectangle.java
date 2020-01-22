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

import javafx.scene.Cursor;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 *
 * @author P.C.T. Kolkman
 */
public class GuideRectangle extends Rectangle {
    
    public static enum GuideOrientation{TOP, RIGHT, BOTTOM, LEFT};
    
    private Rectangle adjRectangle;
    private GuideOrientation or;
    private double wGRect, hGRect, wPRect, hPRect, yPRect, xPRect;
    private double xInit, yInit;
    private boolean interacting;
    
    public GuideRectangle(Rectangle forLayoutAdjustment, GuideOrientation relPosition){
        adjRectangle = forLayoutAdjustment;
        or = relPosition;
        
        interacting = false;
        setFill(adjRectangle.getStroke());        
        setStroke(Color.TRANSPARENT);
        
        wGRect = 10.0;
        hGRect = 40.0;
        switch(or){
            case TOP:
            case BOTTOM:
                wGRect = 40.0;
                hGRect = 10.0;
                break;
        }
        setWidth(wGRect);
        setHeight(hGRect);
        
        wPRect = 0.0; hPRect = 0.0; 
        yPRect = 0.0; xPRect = 0.0; 
        
        takePosition();
        
        setOnMouseEntered((mE)->{
            switch(or){
                case TOP:
                case BOTTOM:
                    setCursor(Cursor.V_RESIZE);
                    break;
                case LEFT:
                case RIGHT:
                    setCursor(Cursor.H_RESIZE);
                    break;
            }        
        });
        
        
        setOnMousePressed((mP)->{
            xInit = mP.getSceneX();
            yInit = mP.getSceneY();
            wPRect = adjRectangle.getBoundsInLocal().getWidth() -
                    (2.0*adjRectangle.getStrokeWidth());
            hPRect = adjRectangle.getBoundsInLocal().getHeight() -
                    (2.0*adjRectangle.getStrokeWidth()); 
            xPRect = adjRectangle.getTranslateX(); 
            yPRect = adjRectangle.getTranslateY();
            interacting = true;
        });
        setOnMouseReleased((mR)->{
            interacting = false;
        });
        setOnMouseDragged((mD)->{
            double dPix;            
            switch(or){
                case TOP:
                    dPix = yInit - mD.getSceneY();
                    adjRectangle.setHeight(hPRect + 
                            dPix);
                    adjRectangle.setTranslateY(yPRect - dPix);
                    break;
                case RIGHT:
                    dPix =  mD.getSceneX() - xInit;
                    adjRectangle.setWidth(wPRect + 
                            dPix);
                    break;
                case BOTTOM:
                    dPix = mD.getSceneY() - yInit;
                    adjRectangle.setHeight(hPRect + 
                            dPix);
                    break;
                case LEFT:
                    dPix =  mD.getSceneX() - xInit;
                    adjRectangle.setWidth(wPRect - 
                            dPix);
                    adjRectangle.setTranslateX(xPRect + dPix);
                    break;
            }
            ShrinkPiece.updatePaperFrameGuides();
        });
    }
    
    public void takePosition(){
        switch(or){
            case TOP:
                setTranslateX(adjRectangle.getBoundsInParent().getMinX() +
                        (adjRectangle.getBoundsInLocal().getWidth()/2.0)
                        - (wGRect/2.0));
                setTranslateY(adjRectangle.getBoundsInParent().getMinY() -
                        (hGRect/2.0));
                break;
            case RIGHT:
                setTranslateX(adjRectangle.getBoundsInParent().getMaxX() -
                        (wGRect/2.0));
                setTranslateY(adjRectangle.getBoundsInParent().getMinY() -
                        (hGRect/2.0) +
                        (adjRectangle.getBoundsInLocal().getHeight()/2.0));
                break;
            case BOTTOM:
                setTranslateX(adjRectangle.getBoundsInParent().getMinX() +
                        (adjRectangle.getBoundsInLocal().getWidth()/2.0)
                        - (wGRect/2.0));
                setTranslateY(adjRectangle.getBoundsInParent().getMaxY() -
                        (hGRect/2.0));
                break;
            case LEFT:
                setTranslateX(adjRectangle.getBoundsInParent().getMinX() -
                        (wGRect/2.0));
                setTranslateY(adjRectangle.getBoundsInParent().getMinY() -
                        (hGRect/2.0) +
                        (adjRectangle.getBoundsInLocal().getHeight()/2.0));
                break;
        }
    }
    
    public boolean isInteracting(){
        return interacting;
    }
    
    public double getWidthGuide(){
        return wGRect;
    }
    public double getHeightGuide(){
        return hGRect;
    }
    public GuideOrientation getGuideOrientation(){
        return or;
    }
    
}
