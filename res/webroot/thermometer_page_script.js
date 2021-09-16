const serverURL = 'ws://localhost:8081';
const PERIOD = Math.random()*10000 + 15000;
//const serverURL = 'ws://1104-5-170-128-165.ngrok.io'
let webSocket;

let node;

let connectionsManager;

window.onload = function(){
    webSocket = new WebSocket(serverURL);
    setUpSocket(webSocket);
    connectionsManager = new WebrtcManager(webSocket);
    setInterval(sensorsRilevation, PERIOD);
}

window.onbeforeunload = function(){
    connectionsManager.sendMsgToConnectedNodes(JSON.stringify({"type": "close_connection"}));
};


function setUpSocket(webSocket){
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
                            node = new GenericNode(json.id, json.node_configuration, webSocket, connectionsManager);
                            connectionsManager.setStrategy(new ChannelStrategy(connectionsManager, node));
                            if(node.ifConfigurationValid()){
                                node.sendNodeConfiguration();
                                console.log("Node is valid");
                                node.setNewCoordinates(json.x, json.y);
                                
                            }else{
                                console.error("Node configuration is not valid");
                                node = null;
                            }
                        }else{
                            console.error("Can't create a new node, because it is already exists");
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
            node.setValue(json.value, json.sensor_name);
    
        }else if(json.device_type === "actuator"){
            node.setValue(json.value, json.actuator_name)
        }else{
            console.error("Unknown mode for change the node's state", json.device_type);
        }
    }else{
        console.error("Wrong id", json.id);
    }
}

function sensorsRilevation(){
    if (typeof node !== 'undefined') {
        let sensors = node.getSensors();
        if(sensors.length > 0){
            let sensor = sensors[Math.floor(Math.random() * sensors.length)];
            let plusOrMinus = Math.random() < 0.5 ? -1 : 1;
            switch(sensor.getValueType()){
                case "real":
                    node.setValue(sensor.getValue() + Math.random()*plusOrMinus, sensor.sensor_name);
                    break;
                case "integer":
                    node.setValue(sensor.getValue() + plusOrMinus, sensor.sensor_name);
                    break;
                case "natural":
                    if(sensor.getValue() + plusOrMinus < 0){
                        node.setValue(sensor.getValue() + 1, sensor.sensor_name);
                    }else{
                        node.setValue(sensor.getValue() + plusOrMinus, sensor.sensor_name);
                    }
                    break;
                case "boolean":
                    node.setValue(!sensor.getValue(), sensor.sensor_name);
                    break;
                default:
                    console.error("Type of sensor value was not recognized");
            }  
        }       
    }
}
