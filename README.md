# WebNodes
WebNodes is a system that allows you to remotely create, configure and manage logical nodes that simulate physical devices. 
Each logical node is rapresented by a single web page. 
Nodes that are close to each other will be automatically interconnected via WebRTC.

## How to import the project in Eclipse
Clone the repository and go to `File -> Import -> Maven -> Existing Maven Projects`.

## How to launch
1. Launch server `src/main/Main.java`
2. Open a generator web page `http://localhost:8081/static/index.html` 
3. Use API
4. Your browser may block opening of new tabs, you need to allow it by clicking on the button at the end of the browser's URL bar. 

## How to test
For each test:
  1. Open a generator web page `http://localhost:8081/static/index.html` 
  2. Execute test

Too short delay could provocate faling of the tests
