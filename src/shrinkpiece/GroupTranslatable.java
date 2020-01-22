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
import javafx.scene.Group;

/**
 *
 * @author P.C.T. Kolkman
 */
public class GroupTranslatable extends Group {
    
    double xPosPreDrag, yPosPreDrag, mXPreDrag, mYPreDrag;

    public GroupTranslatable(){
        setOnMouseEntered((mE)->{            
            setCursor(Cursor.MOVE);
        });
        setOnMouseExited((mEx)->{            
            setCursor(null);
        });
        setOnMousePressed((mP)->{
            xPosPreDrag = getTranslateX();
            yPosPreDrag = getTranslateY();
            mXPreDrag = mP.getSceneX();
            mYPreDrag = mP.getSceneY();
        });
        setOnMouseDragged((mD)->{
            setTranslateX(xPosPreDrag + (mD.getSceneX() - mXPreDrag));
            setTranslateY(yPosPreDrag + (mD.getSceneY() - mYPreDrag));
        });
    }
    
}
