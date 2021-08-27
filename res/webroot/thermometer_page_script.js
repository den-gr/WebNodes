const serverURL = 'ws://localhost:8081';

let webSocket;

let node;

let temperatureUpdate;

window.onload = function(){
	setUpSocket();
    temperatureUpdate = setInterval(() => {
        var plusOrMinus = Math.random() < 0.5 ? -1 : 1;
        node.setTemperature(node.getTemperature() + Math.random()*plusOrMinus);
    }, Math.random()*10000 + 5000);
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
                            node = new ThermometerNode(json.id, json.x, json.y, webSocket);
                        }else{
                            console.error("Can't create a new node, because it is already exists")
                        }
                        break;
                    case "notify_state":
                        node.notifyState();
                        break;
                    case "set_temperature":
                        json.id == node.id ? node.setTemperature(json.temperature) : console.error("Wrong id");
                        break;
                    case "move_node":
                        json.id == node.id ? node.setNewCoordinates(json.x, json.y) : console.error("Wrong id");
                        break;
                    case "disconnect_node":
                        json.id == node.id ? window.close() : console.error("Wrong id");
                        break;
                    default:
                        console.error("Incorrect message type");
                }
			} 
		}catch(err){
			console.error(err);
		}
	};
}