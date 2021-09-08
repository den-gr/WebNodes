class ChannelStrategy{
	constructor(observer, node){
		this.observer = observer;
		this.node = node;
	}

	onOpen(channel, id){
        console.log("my channel id in onopen: " + id );
		console.log("Open a new P2P connection");
		this.observer.notifyConnectionComplete();
		channel.send(JSON.stringify({"type": "hello", "id": this.node.id}))
	}

	onMessage(msg, channel, id){
		console.log("A messge from p2p channel " + id + ":", JSON.parse(msg.data));
		let json = JSON.parse(msg.data);
		if(json.type == "hello"){
			console.log("Hello type");
			this.node.addConnectedNode(id, json.id);
		}else if(json.type == "movementTo"){
			if(this.node.isOutOfRadius(json.x, json.y)){
				console.log("out of radius");
				this.node.removeConnectedNode(id);
				channel.close();
			}
		}
	}

	onClose(channel, id){
		console.log("Close a P2P connection"); 
		this.node.removeConnectedNode(id);
	}

	onDisconnected(channel, id){
		console.log("disconnected");
		this.node.removeConnectedNode(id);
	}

}