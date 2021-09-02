const LED_ON_CLASS = "led_on";

class ThermometerNode{
   
    
    constructor(id, x, y, webSocket){
        this.id = id;
        this.x = x;
        this.y = y;
        this.temperature = 25;
        this.connectedNodes = new Map();
        this.webSocket = webSocket;
        
        document.title = "Thermometer " + this.id;
        document.getElementById("h1").innerHTML += " " + this.id;

        this.temperatureLabel = document.getElementById("t");
        this.xLabel = document.getElementById("xx");
        this.yLabel = document.getElementById("yy");
        this.ledLabel = document.getElementById("led");

        this.setNewCoordinates(x,y);
        this.setTemperature(this.temperature);

        document.getElementById("node_container").style.visibility = "visible";

        this.#sendNodeConfiguration();
    }

    setTemperature(newTemperature){
        if(newTemperature > -100 && newTemperature < 100){
            this.temperature =  Math.round(newTemperature * 100) / 100;
            this.temperatureLabel.innerText = this.temperature;
            this.notifyState();
        }else{
            console.log("The new temperature is out of range");
        }
    }

    turnOnLed(turnOn){
       
        //this.ledLabel.style.backgroundColor = turnOn ? "red" : "gray";
        if(turnOn && !this.ledLabel.classList.contains(LED_ON_CLASS)){
            this.ledLabel.classList.add(LED_ON_CLASS);
        }else if(!turnOn && this.ledLabel.classList.contains(LED_ON_CLASS)){
            this.ledLabel.classList.remove(LED_ON_CLASS);
        }
    }

    getTemperature(){
        return this.temperature;
    }

    #sendNodeConfiguration(){
        let obj = {
            "type": "node_configuration",
            "id": this.id,
            "sensors": [
                {"sensor_name": "thermometer",
                "value_name": "temperature",
                "value_type": "real"}
            ],
            "actuators": [
                {"actuator_name": "led",
                "value_name": "turn_on",
                "value_type": "boolean"}
            ],
            
        };
        this.webSocket.send(JSON.stringify(obj));
    }

    notifyState(){
        let arr = [];
        let it = this.connectedNodes.values();

        let result = it.next();
        while (!result.done) {
            arr.push(result.value);
            result = it.next();
        }
        
        let obj = JSON.stringify(
            {"type": "node_state",
            "id": this.id, 
            "temperature": this.temperature, 
            "x": this.x, 
            "y": this.y,
            "led_on": this.ledLabel.classList.contains(LED_ON_CLASS),
            "connected_nodes_id": arr
            });
        webSocket.send(obj);
    }

    setNewCoordinates(x, y){
        console.log("change coor")
        this.x = x;
        this.y = y;
        this.xLabel.innerText = x;
        this.yLabel.innerText = y;
        this.notifyState();
    }

    addConnectedNode(channelId, nodeId){
        console.log("Before add", this.connectedNodes);
        this.#print();
        this.connectedNodes.set(channelId, nodeId);
        console.log("After add", this.connectedNodes);
        this.#print();
        this.notifyState();
    }

    removeConnectedNode(channelId){
        this.connectedNodes.delete(channelId);
        this.notifyState();
    }

    #print(){
        for (var i = 0, keys = Object.keys(this.connectedNodes), ii = keys.length; i < ii; i++) {
            console.log(keys[i] + '|' + map[keys[i]].list);
        }
    }
}