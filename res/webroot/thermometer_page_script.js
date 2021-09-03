const serverURL = 'ws://localhost:8081';

let webSocket;

let node;

let temperatureUpdate;

let connectionsManager;

window.onload = function(){
    connectionsManager = new WebrtcManager();
	setUpSocket();
    temperatureUpdate = setInterval(() => {
        var plusOrMinus = Math.random() < 0.5 ? -1 : 1;
        node.setTemperature(node.getTemperature() + Math.random()*plusOrMinus);
    }, Math.random()*10000 + 15000);
}


function setUpSocket(){
	webSocket = new WebSocket(serverURL);

	webSocket.onopen = () => {
            console.log('open');
            //ask for node setup
            webSocket.send(JSON.stringify({"type": "node_setup_demand"}));
    }
	webSocket.onclose = () => console.log('close');
	webSocket.onerror = () => console.log('error', error.message);

	webSocket.onmessage = function(msg) {
		console.log('message: ', msg.data);
        try{
			let json = JSON.parse(msg.data);
			//console.log('JSON: ', json);
			if(json.type != null){
                switch(json.type){
                    case "node_setup":
                        if(node == null){
                            node = new ThermometerNode(json.id, json.x, json.y, json.radius, webSocket, connectionsManager);
                        }else{
                            console.error("Can't create a new node, because it is already exists")
                        }
                        break;
                    case "notify_state":
                        node.notifyState();
                        break;
                    case "move_node":
                        json.id == node.id ? node.setNewCoordinates(json.x, json.y) : console.error("Wrong id");
                        break;
                    case "disconnect_node":
                        json.id == node.id ? window.close() : console.error("Wrong id");
                        break;
                    case "change_node_state":
                        changeNodeState(json);
                        break;
                    case "connection_available":
                        connectionsManager.connectionAvailable();
                        break;
                    default:
                        console.error("Incorrect message type");
                }
			}else if(json.desc || json.candidate){
                connectionsManager.elaborateMsg(json);
            }else{
                console.error("Message unrecognized", json);
            } 
		}catch(err){
			console.error(err);
		}
	};
}



function changeNodeState(json){
    if(json.id == node.id){
        if(json.device_type === "sensor"){
            if(json.sensor_name === "thermometer"){
                node.setTemperature(json.temperature);
            }else{
                console.error("Unknown sensor", json.sensor_name);
            }
    
        }else if(json.device_type === "actuator"){
            if(json.actuator_name === "led"){
                node.turnOnLed(json.turn_on);
            }else{
                console.error("Unknown actuator", json.sensor_name);
            }
        }else{
            console.error("Unknown mode for change the node's state", json.device_type);
        }
    }else{
        console.error("Wrong id", json.id);
    }
}