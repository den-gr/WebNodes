class GenericNode{
   static CONNECTIONS = "CONNECTIONS";
    constructor(id, json_configuration, webSocket, webrtcManager){
        this.isSetUp = false;
        this.id = id; 
        this.x = 0;
        this.y = 0;
        this.connectedNodes = new Map();

        let conf = json_configuration;

        if( conf.hasOwnProperty("sensors") && conf.hasOwnProperty("actuators") && conf.hasOwnProperty("radius")){
            if(Number.isInteger(conf.radius) && conf.radius > 0){
                this.radius = conf.radius;
            }else{
                console.error("Not accept configuration radius");
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
                console.error("Not accept configuration of sensors or actuators");
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
            console.error("Configuration failed");
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

    getSensors(){
        return this.sensors;
    }

    #findDevice(device_name){
        for(let i = 0; i < this.sensors.length; i++){
            if(this.sensors[i].sensor_name === device_name){
                //console.log("find sensor");
                return this.sensors[i];
            }
        }
        for(let i = 0; i < this.actuators.length; i++){
            if(this.actuators[i].actuator_name === device_name){
                //console.log("find actuator");
                return this.actuators[i];
            }
        }
        console.error("device not found");
        return null;
    }

    notifyState(type){
        let stateJSON;
        if(type === GenericNode.CONNECTIONS){
            stateJSON = 
            {"type": "node_state",
            "id": this.id, 
            "connected_nodes_id": this.#getArrayWithConnectedNodesId()
            };
        }else{
            stateJSON = 
            {"type": "node_state",
            "id": this.id, 
            "x": this.x, 
            "y": this.y,
            "connected_nodes_id": this.#getArrayWithConnectedNodesId()
            };
            let devices_state = {};
            this.sensors.forEach(sensor => {
                devices_state[sensor.value_name] = sensor.value;
            });
            this.actuators.forEach(actuator => {
                devices_state[actuator.value_name] = actuator.value; 
            });
            stateJSON["devices_state"] =  devices_state;
        }
        
        this.webSocket.send(JSON.stringify(stateJSON));
    }

    setNewCoordinates(x, y){
        this.x = x;
        this.y = y;
        this.xLabel.classList.add('pre-animation');
        this.yLabel.classList.add('pre-animation');
           
        setTimeout(func, 1000, this.xLabel, x);
        setTimeout(func, 1000, this.yLabel, y);
        this.webrtcManager.sendMsgToConnectedNodes(JSON.stringify({"type": "movementTo", "x": this.x, "y": this.y}));
        this.notifyState();
    }

    addConnectedNode(channelId, nodeId){
        this.connectedNodes.set(channelId, nodeId);
        this.notifyState(GenericNode.CONNECTIONS);

        let li = document.createElement('li');
        li.id = "nodeId" + nodeId;
        li.innerText = nodeId;
        li.classList.add("node");
        this.connectedNodesLable.appendChild(li)
    }

    removeConnectedNode(channelId){
        if(typeof this.connectedNodes.get(channelId) !== "undefined"){
            let elem = document.getElementById("nodeId" + this.connectedNodes.get(channelId));
            elem.remove();
            this.connectedNodes.delete(channelId);
            this.notifyState(GenericNode.CONNECTIONS);
        }   
    }

    //return true if a point with coordinates (nodeX; nodeY) is outside of radius of node
    isOutOfRadius(nodeX ,nodeY){
        return Math.hypot(this.x-nodeX, this.y-nodeY) > this.radius;
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
}

class Device{

    constructor(value_name, value_type){
        this.value_name = value_name;
        this.value_type = value_type;
        this.setValue(value_type === "boolean" ? false : 0);

        // this.func = sp => {
        //     sp.classList.remove('pre-animation')
        //     sp.innerText = this.value;
            
        // }
    }


    setValue(new_value){
        switch(this.value_type){
            case "real":
                this.value = Math.round(new_value * 100) / 100;
                break;
            case "integer":
                this.value = Math.floor(new_value);
                break;
            case "natural":
                this.value  = new_value < 0 ? 0 : new_value;
                break;
            case "boolean":
                this.value  = typeof variable == "boolean" ? new_value : new_value > 0 ;
                break;
            default:
                console.error("Type of sensor value was not recognized");
        }
        if(this.span){
            this.span.classList.add('pre-animation');
           
            setTimeout(func, 1000, this.span, this.value);
        }
       
    }

  
  

    getValue(){
        return this.value;
    }

    getValueType(){
        return this.value_type;
    }

    setHTML(name, sensor){
        let p = document.createElement('li');
        this.span = document.createElement('span');
        this.span.id = this.value_name;
        this.span.innerText = this.value;
        p.innerText = "["+ name.toUpperCase() + "]: " + this.value_name + " ";
        p.appendChild(this.span);
        
        let html_list_id = sensor ? "sensors_list" : "actuators_list";
        document.getElementById(html_list_id).appendChild(p);
    }
}

class Sensor extends Device{
    
    constructor(sensor_name, value_name, value_type){
        super(value_name, value_type);
        this.sensor_name = sensor_name;
        super.setHTML(this.sensor_name, true);
    }
}

class Actuator extends Device{
    
    constructor(actuator_name, value_name, value_type){
        super(value_name, value_type);
        this.actuator_name = actuator_name;
        super.setHTML(this.actuator_name, false);
       
    }
}

function func(sp, vl){
    sp.classList.remove('pre-animation')
    sp.innerText = vl;
    
}