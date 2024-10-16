package ru.rainman.metamasksdktest.repo.model;

import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.*;

public class ChannelMeta extends DynamicStruct {
    public Uint256 _timestamp;

    public Uint256 _donateChannel;

    public Uint64 _cnlIndex;

    public DynamicBytes _description;

    public DynamicBytes _title;

    public Uint64 _shotsCount;

    public Uint256 _channelPrice;

    public Address _channelAddress;

    public Bool _isClosed;

    public ChannelMeta(Uint256 _timestamp, Uint256 _donateChannel, Uint64 _cnlIndex,
                       DynamicBytes _description, DynamicBytes _title, Uint64 _shotsCount,
                       Uint256 _channelPrice, Address _channelAddress, Bool _isClosed) {
        super(_timestamp, _donateChannel, _cnlIndex, _description, _title, _shotsCount, _channelPrice, _channelAddress, _isClosed);
        this._timestamp = _timestamp;
        this._donateChannel = _donateChannel;
        this._cnlIndex = _cnlIndex;
        this._description = _description;
        this._title = _title;
        this._shotsCount = _shotsCount;
        this._channelPrice = _channelPrice;
        this._channelAddress = _channelAddress;
        this._isClosed = _isClosed;
    }
}