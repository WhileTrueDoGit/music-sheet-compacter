#ShrinkPiece
This project is about shaping standard music sheets to marching band music sheets.

_A4 music sheets, can be converted into compact (landscape oriented) A5 music sheets._

##Methodology
The currently used language is Java, JDK 1.8. The project is mainly based on JavaFX imports.

##Current usage
1. In the current implementation, music sheets are imported as screen prints.
2. The image can be cropped.
3. Width-scaling is applied to fit in to the (landscape or portrait) oriented A5 paper size.
4. Fully blank (white or an adjustable paper-color) horizontal lines become eligible for deletion by the user.
_This aids to create vertical space._
5. a. Additional spots can be manually deleted (step 4. will be repeated). 
5. b. Repetition signs can be added (to remove unnecessary music bars).
5. c. Text can be randomly placed (to [re]move bulky titles).
6. a. Repeat the process (starting from 1.) with a new (excerpt of) music sheet.
6. b. Print the result (to PDF).

##Notes
As this project was initiated to help a local orchestra, the current user-interface language is Dutch.
Conversion into English will be the first issue raised on this project.