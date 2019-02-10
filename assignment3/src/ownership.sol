pragma solidity >=0.4.22 <0.6.0;

contract Ownership {

    // the structure of our item, swarm_info can be used by web apps to store and load additional
    // information from a swarm file that can be saved as a json file for example and contain its
    // own structure depending on the app. This way we provide dynamic interface for apps
    struct Item {
        address payable owner;
        uint timestamp;
        bool sellable;
        uint price;
        string swarm_hash;
    }
  
    // since we do not know the length of the used hash, we map a dynamic string to our Item
    // this way the user can map a regular hash or a swarm hash for example to his item
    mapping(string => Item) items;
   
    // Nothing needs to be done here
    constructor() public {

    }
   
    modifier onlyOwner(string memory _hash) {
        require(
            msg.sender == items[_hash].owner,
            "Only the current owner can do this."
        );
        _;
    }
   
    // Here we create our item and map the hash to the item
    function createItem(string memory _hash, bool _sellable, uint _price, string memory _swarm_hash ) public {
        // if the item does not exist, this will always be the address
        require(
            items[_hash].owner == 0x0000000000000000000000000000000000000000,
            "Item already exists."
        );

        items[_hash].owner = msg.sender;
        items[_hash].sellable = _sellable;
        if (_sellable){
            items[_hash].price = _price;
        } else {
            items[_hash].price = 0;
        }
        items[_hash].swarm_hash = _swarm_hash;
        // timestamp is fixed and can be used as a proof that whatever is behind that hash existed since at least this timestamp
        items[_hash].timestamp = block.timestamp;
    }

    // onlyOwner checks if owner is sender and also checks if item exists
    function makeItemSellable(string memory _hash, uint _price) public onlyOwner(_hash) {
        // Set sellable to true
        items[_hash].sellable = true;
       
        // Finally, set the price
        items[_hash].price = _price;
    }

    function makeItemNotSellable(string memory _hash) public onlyOwner(_hash) {
        // Set sellable to false
        items[_hash].sellable = false;
        items[_hash].price = 0;
    }

    function buyItem(string memory _hash) payable public {
        // At first, check if the item is sellable
        require(
            items[_hash].sellable == true,
            "The item cannot be sold."
        );
       
        // Check if the amount of ethers is high enough
        require(
            msg.value == items[_hash].price,
            "Wrong Price!"
        );
       
        // Check if the message sender is not the current owner
        require(
            msg.sender != items[_hash].owner,
            "The current owner cannot buy the item."
        );

        items[_hash].owner.transfer(msg.value);
        items[_hash].owner = msg.sender;
        items[_hash].sellable = false;
        items[_hash].price = 0;
    }

    // In case the owner wants to transfer the item to a new owner
    function transferItem(string memory _hash, address payable new_owner) public onlyOwner(_hash) {
        // Receive ethers
        items[_hash].owner = new_owner;
        items[_hash].sellable = false;
        items[_hash].price = 0;
    }

    // function for a webapp to view an item/hash
    function infoItem(string memory _hash) public view returns (address, bool, uint, string memory, uint ){
        // Receive ethers
        return (items[_hash].owner, items[_hash].sellable, items[_hash].price, items[_hash].swarm_hash, items[_hash].timestamp );
    }
 
    function setSwarmInfo(string memory _hash, string memory _swarm_hash) public onlyOwner(_hash) {
        items[_hash].swarm_hash = _swarm_hash;
    }
 
    // in case the item should be deleted, deleting is good as the ethereum virtual machine needs to store less.
    function deleteItem(string memory _hash) public onlyOwner(_hash) {
        delete items[_hash];
    }
}