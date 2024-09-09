@rest service TravelExtensionService {

  action OnTravelCreated (travel : Travel);
  action OnTravelChanged (travel : Travel);
  action OnTravelRejected (travel : Travel);

  type Travel {
    TravelID       : Integer;
    CustomerID     : Integer;
    BeginDate      : Date;
    EndDate        : Date;
    BookingFee     : Decimal;
    TotalPrice     : Decimal;
  }
}
