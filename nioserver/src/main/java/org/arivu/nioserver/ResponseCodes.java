package org.arivu.nioserver;

/**
 * List of all response codes.
 * 
 * @author Mr P
 *
 */
public interface ResponseCodes {
	public int Continue = 100;
	public int Switching_Protocols = 101;
	public int Processing = 102;
	public int Checkpoint = 103;
	public int OK = 200;
	public int Created = 201;
	public int Accepted = 202;
	public int Non_Authoritative_Information = 203;
	public int No_Content = 204;
	public int Reset_Content = 205;
	public int Partial_Content = 206;
	public int Multi_Status = 207;
	public int Already_Reported = 208;
	public int IM_Used = 226;
	public int Multiple_Choices = 300;
	public int Moved_Permanently = 301;
	public int Found = 302;
	public int See_Other = 303;
	public int Not_Modified = 304;
	public int Use_Proxy = 305;
	public int Switch_Proxy = 306;
	public int Temporary_Redirect = 307;
	public int Permanent_Redirect = 308;
	public int Bad_Request = 400;
	public int Unauthorized = 401;
	public int Payment_Required = 402;
	public int Forbidden = 403;
	public int Not_Found = 404;
	public int Method_Not_Allowed = 405;
	public int Not_Acceptable = 406;
	public int Proxy_Authentication_Required_ = 407;
	public int Request_Timeout = 408;
	public int Conflict = 409;
	public int Gone = 410;
	public int Length_Required = 411;
	public int Precondition_Failed = 412;
	public int Payload_Too_Large = 413;
	public int URI_Too_Long_ = 414;
	public int Unsupported_Media_Type = 415;
	public int Range_Not_Satisfiable = 416;
	public int Expectation_Failed = 417;
	public int Im_a_teapot = 418;
	public int Im_a_fox = 419;
	public int Method_Failure = 420;
	public int Misdirected_Request = 421;
	public int Unprocessable_Entity = 422;
	public int Locked = 423;
	public int Failed_Dependency = 424;
	public int Upgrade_Required = 426;
	public int Precondition_Required = 428;
	public int Too_Many_Requests = 429;
	public int Request_Header_Fields_Too_Large = 431;
	public int Login_Timeout = 440;
	public int No_Response = 444;
	public int Retry_With = 449;
	public int Blocked_by_Windows_Parental_Controls = 450;
	public int Unavailable_For_Legal_Reasons = 451;
	public int SSL_Certificate_Error = 495;
	public int SSL_Certificate_Required = 496;
	public int HTTP_Request_Sent_to_HTTPS_Port = 497;
	public int Invalid_Token = 498;
	public int Request_has_been_forbidden_by_antivirus = 499;
	public int Internal_Server_Error = 500;
	public int Not_Implemented = 501;
	public int Bad_Gateway = 502;
	public int Service_Unavailable = 503;
	public int Gateway_Timeout = 504;
	public int HTTP_Version_Not_Supported = 505;
	public int Variant_Also_Negotiates = 506;
	public int Insufficient_Storage = 507;
	public int Loop_Detected = 508;
	public int Bandwidth_Limit_Exceeded = 509;
	public int Not_Extended = 510;
	public int Network_Authentication_Required = 511;
	public int Unknown_Error = 520;
	public int Web_Server_Is_Down = 521;
	public int Connection_Timed_Out = 522;
	public int Origin_Is_Unreachable = 523;
	public int A_Timeout_Occurred = 524;
	public int SSL_Handshake_Failed = 525;
	public int Invalid_SSL_Certificate = 526;
	public int Railgun_Error = 527;
	public int Site_is_frozen = 530;
}
