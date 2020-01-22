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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 *
 * @author P.C.T. Kolkman
 */
public class ColorThreshold extends Group {
    
    private ObjectProperty<Color> myColProperty;
    private Rectangle colRepr, shadeOverCTh;
    private ColorPicker thColPicker;
    private DropShadow cThShadow;
    private Label thLabel;
    private Group thButtWithLabel;
    private double xOffSh, yOffSh;
    private boolean cThPressed;
    
    public ColorThreshold(Color initColor){    
        myColProperty = new SimpleObjectProperty<Color>();
        
        thButtWithLabel = new Group();
        thLabel = new Label("");
        shadeOverCTh = new Rectangle();
        
        cThPressed = false; 
        
        thColPicker = new ColorPicker();
        thColPicker.setVisible(false);
        thColPicker.visibleProperty().addListener(new ChangeListener<Boolean>(){
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean colPVisible) {
                if(colPVisible){                    
                    thColPicker.setValue(getColorThreshold());
                }
            }
        });
        thColPicker.setOnAction((cPAc)->{
            setColorThreshold(thColPicker.getValue());
            thColPicker.setVisible(false);
        });
        
        cThShadow = new DropShadow();
        xOffSh = 7.0; yOffSh = 3.0;
        cThShadow.setOffsetX(xOffSh);
        cThShadow.setOffsetY(yOffSh);
        
        colRepr = new Rectangle();
        colRepr.translateXProperty().addListener(
                (ObservableValue<? extends Number> tXThBox, Number formerTXClThBox,
                Number newTranslXClTh) -> {
            shadeOverCTh.setTranslateX((double)newTranslXClTh);
        });
        colRepr.translateYProperty().addListener(
                (ObservableValue<? extends Number> tYThBox, Number formerTYClThBox,
                Number newTranslYClTh) -> {
            shadeOverCTh.setTranslateY((double)newTranslYClTh);
        });
        colRepr.setTranslateX(0.0);
        colRepr.setTranslateY(0.0);
        colRepr.setWidth(20.0);
        colRepr.setHeight(20.0);
        colRepr.setStroke(Color.BLACK);
        colRepr.setStrokeWidth(2.0);  
        colRepr.setEffect(cThShadow);
        colRepr.setOnMouseEntered((mE)->{
            cThShadow.setOffsetX(xOffSh + 3.0);
            cThShadow.setOffsetY(yOffSh + 2.0);
        });
        colRepr.setOnMouseExited((mEx)->{
            cThShadow.setOffsetX(xOffSh);
            cThShadow.setOffsetY(yOffSh);
        });
        colRepr.setOnMousePressed((mP)->{            
            cThPressed = true;
            
            colRepr.setTranslateX(colRepr.getTranslateX() + xOffSh);
            colRepr.setTranslateY(colRepr.getTranslateY() + yOffSh);
            colRepr.setEffect(null);
            
            shadeOverCTh.setVisible(true);
        });
        colRepr.setOnMouseReleased((mR)->{
            cThPressed = false;
            shadeOverCTh.setVisible(false);            
            thColPicker.setVisible(true);
            
            colRepr.setTranslateX(colRepr.getTranslateX() - xOffSh);
            colRepr.setTranslateY(colRepr.getTranslateY() - yOffSh);
            colRepr.setEffect(cThShadow);
        });
        
        thLabel.setTranslateX(colRepr.getTranslateX());
        thLabel.setTranslateY(colRepr.getTranslateY() + 25.0);
        thButtWithLabel.getChildren().addAll(thLabel, colRepr);
        
        shadeOverCTh.setWidth(20.0 + (colRepr.getStrokeWidth()*2.0));
        shadeOverCTh.setHeight(20.0 + (colRepr.getStrokeWidth()*2.0));
        shadeOverCTh.setFill(Color.WHITE);
        shadeOverCTh.setOpacity(0.5);
        shadeOverCTh.setVisible(false);
        
        getChildren().addAll(thButtWithLabel, shadeOverCTh, thColPicker);        
        
        setColorThreshold(initColor);
    }
    
    public ObjectProperty<Color> colProperty(){
        return myColProperty;
    }
    
    public void setColorThreshold(Color newThreshold){
        myColProperty.set(newThreshold);
        colRepr.setFill(myColProperty.get());
    }
    public Color getColorThreshold(){
        return myColProperty.getValue();
    }    
    
    public void setLabelText(String txtColorThrLabel){
        thLabel.setText(txtColorThrLabel);
        thLabel.setTranslateX(thLabel.getTranslateX() - (0.5*thLabel.
                getBoundsInLocal().getWidth()));
    }    
    
}
