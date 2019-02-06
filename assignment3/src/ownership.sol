pragma solidity >=0.4.22 <0.6.0;

contract Ownership {
    
    address payable public owner;
    bytes32 public hash;
    bool public sellable;
    uint public price;
    
    constructor(bytes32 _hash, bool _sellable, uint _price) public {
        owner = msg.sender;
        addHash(_hash, _sellable, _price);
    }
    
    modifier onlyOwner() {
        require(
            msg.sender == owner,
            "Only the current owner can do this."
        );
        _;
    }
    
    function addHash(bytes32 _hash, bool _sellable, uint _price) private {
        hash = _hash;
        sellable = _sellable;
        price = _price;
    }

    function makeSellable(bytes32 _hash, uint _price) public onlyOwner {
        // At first, check if the provided hash is correct
        require(
            hash == _hash,
            "The provided hash is not correct."
        );
        
        // Set sellable to true
        sellable = true;
        
        // Finally, set the price
        price = _price;
    }

    function makeNotSellable(bytes32 _hash) public onlyOwner {
        // At first, check if the provided hash is correct
        require(
            hash == _hash,
            "The provided hash is not correct."
        );
        
        // Set sellable to true
        sellable = false;
    }

    function buy(bytes32 _hash) payable public {
        // At first, check if the item is sellable
        require(
            sellable == true,
            "The item cannot be sold."
        );
        
        // Check if the amount of ethers is high enough
        require(
            msg.value >= price,
            "The price is higher then the amount sent."
        );
        
        // Check if the provided hash is correct
        require(
            hash == _hash,
            "The provided hash is not correct."
        );
        
        // Receive ethers
        owner.transfer(msg.value);
        
        // Transfer ownership
        transferOwnership(msg.sender);
    }

    function transferOwnership(address payable newOwner) private {
        owner = newOwner;
    }

    /*
    function delete(bytes32 _hash) public onlyOwner {
       
    }
    */
}