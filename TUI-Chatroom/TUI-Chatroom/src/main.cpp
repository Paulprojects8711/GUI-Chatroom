#include <iostream> //Needed for cout and endl 
#include <string>
#include <array>
//Import libary with #include <library name>


using namespace std;

float simpleFloat = 0.3f;
int simpleInt = 4;
string simpleString = "String halt";
bool simpleBool = true;

string simpleArray[5] = { "op1", "op2", "op3", "op3", "op4" };

float num1;
float num2;
float num3;


//Prototypes (Funktion frueher definieren und spaeter aendern, noetig damit man main ganz oben haben kann)
float Addition(float number1, float number2);
float Minimum(float num1, float num2, float num3);


int main() //Function welches ein int zurueck gibt
{
	std::cout << "Hello World" << std::endl; //print

	std::cout << "Enter the First number: "; //Print without new line to enter input in the same line
	std::cin >> num1; //Input from console 
	std::cout << "Enter the Second number: ";
	std::cin >> num2;
	std::cout << "Enter the Third number: ";
	std::cin >> num3;

	//Print the output of Minimum(), convert float to string and combine it with text
	//Converting the float causes rounding errors!
	std::cout << "The smallest number is: " + std::to_string(Minimum(num1, num2, num3)) << std::endl;

	std::cout << Addition(simpleFloat, float(simpleInt)) << std::endl;

	for (string simpleString : simpleArray) //Fuer jedes string in der string Liste
	{
		std::cout << simpleString << std::endl;
	}

	return 0;
}


float Addition(float number1, float number2)
{
	return number1 + number2;
}

float Minimum(float num1, float num2, float num3)
{
	if (num1 < num2 && num1 < num3)
	{
		return num1;
	}
	else if (num2 < num1 && num2 < num3)
	{
		return num1;
	}
	else if (num3 < num1 && num3 < num1)
	{
		return num1;
	}
}