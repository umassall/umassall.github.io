@echo off
deltree /y  %JD%\classes
mkdir       %JD%\classes
copy        %JD%\source\external\paldino\*.class %JD%\classes

javac -g -d %JD%\classes                        %JD%\source\fix\*.java

javac -g -d %JD%\classes                       %JD%\source\Random.java
javac -g -d %JD%\classes              %JD%\source\pointer\Pointer.java
javac -g -d %JD%\classes                 %JD%\source\pointer\PInt.java
javac -g -d %JD%\classes              %JD%\source\pointer\PDouble.java

javac -g -d %JD%\classes         %JD%\source\external\poskanzer\*.java
javac -g -d %JD%\classes      %JD%\source\external\friedmanHill\*.java
javac -g -d %JD%\classes                      %JD%\source\parse\*.java
javac -g -d %JD%\classes                 %JD%\source\expression\*.java
javac -g -d %JD%\classes                     %JD%\source\matrix\*.java
javac -g -d %JD%\classes                    %JD%\source\pointer\*.java
javac -g -d %JD%\classes                      %JD%\source\watch\*.java
javac -g -d %JD%\classes                            %JD%\source\*.java

javac -g -d %JD%\classes                    %JD%\source\picture\*.java
javac -g -d %JD%\classes      %JD%\source\picture\directFractal\*.java

javac -g -d %JD%\classes            %JD%\source\sim\funApp\FunApp.java
javac -g -d %JD%\classes                  %JD%\source\sim\mdp\MDP.java

javac -g -d %JD%\classes                        %JD%\source\sim\*.java
javac -g -d %JD%\classes                 %JD%\source\sim\funApp\*.java
javac -g -d %JD%\classes                    %JD%\source\sim\mdp\*.java
javac -g -d %JD%\classes                   %JD%\source\sim\data\*.java
javac -g -d %JD%\classes                 %JD%\source\sim\errFun\*.java
javac -g -d %JD%\classes               %JD%\source\sim\gradDesc\*.java
javac -g -d %JD%\classes                %JD%\source\sim\display\*.java
