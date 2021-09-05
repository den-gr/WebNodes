class GenericNode{
   
    constructor(id, json_configuration, webSocket, webrtcManager){
        this.isSetUp = false;
        this.id = id; 
        this.x = 0;
        this.y = 0;
        this.temperature = 25;
        this.connectedNodes = new Map();

        let conf = json_configuration;

        if( conf.hasOwnProperty("sensors") && conf.hasOwnProperty("actuators") && conf.hasOwnProperty("radius")){
            if(Number.isInteger(conf.radius) && conf.radius > 0){
                this.radius = conf.radius;
            }else{
                console.log("Not accept configuration radius");
            }

            if(Array.isArray(conf.sensors) && Array.isArray(conf.actuators)){
                this.sensors = [];
                conf.sensors.forEach((item, index)=>{
                    this.sensors.push(new Sensor(item.sensor_name, item.value_name, item.value_type))
                })

                this.actuators = [];
                conf.actuators.forEach((item, index)=>{
                    this.actuators.push(new Actuator(item.actuator_name, item.value_name, item.value_type))
                })


            }else{
                console.log("Not accept configuration of sensors or actuators");
            }
        }

        if( this.hasOwnProperty("sensors") && this.hasOwnProperty("actuators") && this.hasOwnProperty("radius")){
            this.webSocket = webSocket;
            this.webrtcManager = webrtcManager;

            
            this.configuration = conf;
            this.configuration["id"] = this.id;
            document.title = conf.node_name + " " +  this.id;
            document.getElementById("h1").innerHTML =  conf.node_name + " " +  this.id;
            
        }else{
            console.log("Configuration failed");
        }
        
        this.xLabel = document.getElementById("xx");
        this.yLabel = document.getElementById("yy");
        this.connectedNodesLable = document.getElementById("connected_nodes_id");
        document.getElementById("node_container").style.visibility = "visible";
    }

    setValue(new_value, device_name){
        let device = this.#findDevice(device_name);
        if(device != null){
            device.setValue(new_value);
            this.notifyState();
        }
    }

    getValue(device_name){
        let device = this.#findDevice(device_name);
        if(device != null){
            return device.getValue();
        }
    }

    #findDevice(device_name){
        for(let i = 0; i < this.sensors.length; i++){
            if(this.sensors[i].sensor_name === device_name){
                console.log("find sensor");
                return this.sensors[i];
            }
        }
        for(let i = 0; i < this.actuators.length; i++){
            if(this.actuators[i].actuator_name === device_name){
                console.log("find actuator");
                return this.actuators[i];
            }
        }
        console.error("device not found");
        return null;
    }

    notifyState(){
        let obj = 
            {"type": "node_state",
            "id": this.id, 
            "x": this.x, 
            "y": this.y,
            "connected_nodes_id": this.#getArrayWithConnectedNodesId()
            };
        this.sensors.forEach(sensor => {
            obj[sensor.value_name] = sensor.value;
        });
        this.actuators.forEach(actuator => {
            obj[actuator.value_name] = actuator.value;
        });
        webSocket.send(JSON.stringify(obj));
    }

    setNewCoordinates(x, y){
        this.x = x;
        this.y = y;
        this.xLabel.innerText = x;
        this.yLabel.innerText = y;
        this.webrtcManager.sendToAll(JSON.stringify({"type": "movementTo", "x": this.x, "y": this.y}));
        this.notifyState();
    }

    addConnectedNode(channelId, nodeId){
        this.connectedNodes.set(channelId, nodeId);
        this.notifyState();
        this.#updateConnectedNodeslabel();
    }

    removeConnectedNode(channelId){
        this.connectedNodes.delete(channelId);
        this.notifyState();
        this.#updateConnectedNodeslabel();
    }

    //return true if a point with coordinates (nodeX; nodeY) is outside of radius of node
    isOutOfRadius(nodeX ,nodeY){
        return Math.hypot(this.x-nodeX, this.y-nodeY) > this.radius;
    }

    // Update web page
    #updateConnectedNodeslabel(){
        this.connectedNodesLable.innerText = this.#getArrayWithConnectedNodesId(); 
    }

    // return an array with id of the connected nodes
    #getArrayWithConnectedNodesId(){
        let arr = [];
        let it = this.connectedNodes.values();

        let result = it.next();
        while (!result.done) {
            arr.push(result.value);
            result = it.next();
        }
        return arr;
    }

    //Send Node configuration to server
    sendNodeConfiguration(){
        this.webSocket.send(JSON.stringify(this.configuration));
    }

    ifConfigurationValid(){
        return typeof this.configuration !== 'undefined';
    }
}

class Device{

    constructor(value_name, value_type){
        this.value_name = value_name;
        this.value_type = value_type;
        this.value = this.getDefaultValue(this.value_type);
    }

    getDefaultValue(value_type){
        return value_type === "boolean" ? false : 0;

    }

    setValue(new_value){
        //to do type check
        this.value = Math.round(new_value * 100) / 100;
        this.span.innerText = this.value;
    }

    getValue(){
        return this.value;
    }

    setHTML(name){
        let p = document.createElement('p');
        this.span = document.createElement('span');
        this.span.id = this.value_name;
        this.span.innerText = this.value;
        p.innerText = "["+ name.toUpperCase() + "]: " + this.value_name + " ";
        p.appendChild(this.span);
        document.getElementById("node_container").appendChild(p);
    }
}

class Sensor extends Device{
    
    constructor(sensor_name, value_name, value_type){
        super(value_name, value_type);
        this.sensor_name = sensor_name;
        super.setHTML(this.sensor_name);
    }
}

class Actuator extends Device{
    
    constructor(actuator_name, value_name, value_type){
        super(value_name, value_type);
        this.actuator_name = actuator_name;
        super.setHTML(this.actuator_name);
    }
}

