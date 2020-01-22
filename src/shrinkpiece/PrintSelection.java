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
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 *
 * @author P.C.T. Kolkman
 */
public class PrintSelection extends Rectangle {
    
    double initTX, initTY, iMX, iMY, maxXM, maxYM;
    boolean dragged, contextM;
    
    public PrintSelection(double x, double y, double w, double h,
            double maxXMove, double maxYMove, ContextMenu selectorCMenu){
        
        setTranslateX(x);
        setTranslateY(y);
        setWidth(w);
        setHeight(h);
        
        maxXM = maxXMove; maxYM = maxYMove;
        initTX = 0.0; initTY = 0.0; iMX = 0.0;
        iMY = 0.0;
        dragged = false;
        contextM = false;
        
        setFill(Color.TRANSPARENT);
        setStroke(Color.BLACK);
        setStrokeWidth(2.0);
        setOnMouseEntered((mE)->{
            setCursor(Cursor.HAND);
        });
        setOnMouseExited((mE)->{
            setCursor(null);
        });
        setOnMousePressed((mP)->{            
            dragged = false;
            contextM = false;
            if(mP.getButton().equals(MouseButton.SECONDARY)){
                contextM = true;
                PrintSelector.setSelectedPrintSelection(this);
                selectorCMenu.show(this,mP.getScreenX(), mP.getScreenY());
            }else{
                setCursor(Cursor.MOVE);
                initTX = getTranslateX();
                initTY = getTranslateY();
                iMX = mP.getSceneX();
                iMY = mP.getSceneY();   
            }
        });
        setOnMouseDragged((mD)->{
            double boundPos;
            
            dragged = true;
            contextM = false;
            
            boundPos = mD.getSceneX();
            if(mD.getSceneX() > maxXM){
                boundPos = maxXM;
            } else if(boundPos < 0.0){
                boundPos = 0.0;
            }
            setTranslateX(initTX + (boundPos - iMX));
            boundPos = mD.getSceneY();
            if(mD.getSceneY() > maxYM){
                boundPos = maxYM;
            } else if(boundPos < 0.0){
                boundPos = 0.0;
            }
            setTranslateY(initTY + (boundPos - iMY));
        });
        setOnMouseClicked(
                (mR)->{
            if(contains(mR.getX(), mR.getY())){
                setCursor(Cursor.HAND);
            } else {
                setCursor(null);
            }        
            dragged = false;
            contextM = false;
        });
    }
    
    public boolean getDragged(){
        return dragged;
    }
    
    public boolean getContextMenuRequested(){
        return contextM;
    }        
    
    public void flipSides(){        
        double w;
        w = getWidth();
        setWidth(getHeight());
        setHeight(w);
    }
    
}
