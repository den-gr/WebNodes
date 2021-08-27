class ThermometerNode{
    
    constructor(id, x, y, webSocket){
        this.id = id;
        this.x = x;
        this.y = y;
        this.temperature = 25;
        this.webSocket = webSocket;
        
        this.#addToDocument("X coordinate: ", this.x, "x");
        this.#addToDocument("Y coordinate: ", this.y, "y");
        this.#addToDocument("Temperature: ", this.temperature, "t");
        document.title = "Thermometer " + this.id;
        document.getElementById("h1").innerHTML += " " + this.id;

        this.temperatureLabel = document.getElementById("t");
        this.xLabel = document.getElementById("x");
        this.yLabel = document.getElementById("y");

        this.setTemperature(this.temperature);
    }

    setTemperature(newTemperature){
        if(newTemperature > -100 && newTemperature < 100){
            this.temperature =  Math.round(newTemperature * 100) / 100;
            this.temperatureLabel.innerText = "Temperature: " + this.temperature;
            this.notifyState();
        }else{
            console.log("The new temperature is out of range");
        }
    }

    getTemperature(){
        return this.temperature;
    }

    notifyState(){
        let obj = JSON.stringify({"type": "node_state","id": this.id, "temperature": this.temperature, "x": this.x, "y": this.y});
        webSocket.send(obj);
    }

    setNewCoordinates(x, y){
        this.x = x;
        this.y = y;
        this.xLabel.innerText = "X coordinate: " + x;
        this.yLabel.innerHTML = "Y coordinate: " + y;
    }

    #addToDocument(text, value, id){
        let p = document.createElement("p");
        p.innerText = text + value;
        p.id = id;
        document.body.appendChild(p);
        return p;
    }
}