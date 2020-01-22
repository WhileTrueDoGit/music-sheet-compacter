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

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;

/**
 *
 * @author P.C.T. Kolkman
 */
public class GroupButton extends Group {
    
    private final double xOffShadow = 7.0, yOffShadow = 3.0;
    private double xTransl, yTransl;
    private DropShadow buttonShadow;
    private Node ico;
    
    public GroupButton(){
        buttonShadow = new DropShadow();
        buttonShadow.setOffsetX(xOffShadow);
        buttonShadow.setOffsetY(yOffShadow);
        
        ico = null;
        xTransl = 0.0; yTransl = 0.0;
        setOnMouseEntered((mE)->{
            buttonShadow.setOffsetX(xOffShadow + 3.5);
            buttonShadow.setOffsetY(yOffShadow + 1.5);
        });
        setOnMouseExited((mEx)->{
            buttonShadow.setOffsetX(xOffShadow);
            buttonShadow.setOffsetY(yOffShadow);
        });
        setOnMousePressed((mP)->{
            if (getIcon() != null){
                getIcon().setEffect(null);
            }
            xTransl = xOffShadow;
            yTransl = yOffShadow;
            setTranslateX(getTranslateX() + xTransl);
            setTranslateY(getTranslateY() + yTransl);
        });
        setOnMouseReleased((mR)->{
            if (getIcon() != null){
                getIcon().setEffect(buttonShadow);
            }
            setTranslateX(getTranslateX() - xTransl);
            setTranslateY(getTranslateY() - yTransl);            
        });
    }
    
    private void initIcon(){
        ico.setEffect(buttonShadow);        
        getChildren().add(ico);
    }
    
    public void setView(Node buttonIcon){
        ico = buttonIcon;
        initIcon();
    }
    
    private Node getIcon(){
        return ico;
    }
    
}
