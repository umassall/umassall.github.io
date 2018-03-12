@echo off
deltree /y  %JD%\classes
mkdir %JD%\classes
mkdir %JD%\classes\sim
mkdir %JD%\classes\sim\display
copy %JD%\source\external\paldino\*.class %JD%\classes

javac -O -d %JD%\classes                  %JD%\source\Random.java
javac -O -d %JD%\classes    %JD%\source\external\poskanzer\*.java
javac -O -d %JD%\classes %JD%\source\external\friedmanHill\*.java
javac -O -d %JD%\classes         %JD%\source\pointer\Pointer.java
javac -O -d %JD%\classes            %JD%\source\pointer\PInt.java
javac -O -d %JD%\classes         %JD%\source\pointer\PDouble.java
javac -O -d %JD%\classes                   %JD%\source\fix\*.java
javac -O -d %JD%\classes                 %JD%\source\parse\*.java
javac -O -d %JD%\classes            %JD%\source\expression\*.java
javac -O -d %JD%\classes                %JD%\source\matrix\*.java
javac -O -d %JD%\classes               %JD%\source\pointer\*.java
javac -O -d %JD%\classes                 %JD%\source\watch\*.java
javac -O -d %JD%\classes                   %JD%\source\Alert.java
javac -O -d %JD%\classes %JD%\source\GWin.java %JD%\source\sim\display\Display.java %JD%\source\sim\display\DisplayList.java
javac -O -d %JD%\classes                       %JD%\source\*.java
javac -O -d %JD%\classes               %JD%\source\picture\*.java
javac -O -d %JD%\classes %JD%\source\picture\directFractal\*.java
javac -O -d %JD%\classes       %JD%\source\sim\funApp\FunApp.java
javac -O -d %JD%\classes             %JD%\source\sim\mdp\MDP.java
javac -O -d %JD%\classes          %JD%\source\sim\Experiment.java
javac -O -d %JD%\classes       %JD%\source\sim\errFun\ErrFun.java
javac -O -d %JD%\classes   %JD%\source\sim\gradDesc\GradDesc.java
javac -O -d %JD%\classes                   %JD%\source\sim\*.java
javac -O -d %JD%\classes            %JD%\source\sim\funApp\*.java
javac -O -d %JD%\classes               %JD%\source\sim\mdp\*.java
javac -O -d %JD%\classes              %JD%\source\sim\data\*.java
javac -O -d %JD%\classes            %JD%\source\sim\errFun\*.java
javac -O -d %JD%\classes          %JD%\source\sim\gradDesc\*.java
javac -O -d %JD%\classes           %JD%\source\sim\display\*.java

