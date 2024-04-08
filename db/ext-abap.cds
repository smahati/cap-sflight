namespace ext.abap;

@cds.persistence.exists
entity Airline {
  key AirlineID : String(3);
  Name          : String(40);
  //CurrencyCode  : Currency;
  CurrencyCode_code  : String(3);
  AirlinePicURL : String;
};

@cds.persistence.exists
entity TravelAgency {
  key AgencyID : String(6);
  Name         : String(80);
  Street       : String(60);
  PostalCode   : String(10);
  City         : String(40);
  //CountryCode  : Country;
  CountryCode_code : String(3);
  PhoneNumber  : String(30);
  EMailAddress : String(256);
  WebAddress   : String(256);
};

@cds.persistence.exists
@external.abap: { dbname, schema }
entity Passenger {
  key CustomerID : String(6);
  FirstName      : String(40);
  LastName       : String(40);
  Title          : String(10);
  Street         : String(60);
  PostalCode     : String(10);
  City           : String(40);
  //CountryCode    : Country;
  CountryCode_code : String(3);
  PhoneNumber    : String(30);
  EMailAddress   : String(256);
};
