from google.appengine.runtime.apiproxy_errors import CapabilityDisabledError
from google.appengine.ext import ndb
from flask_restful import reqparse, marshal_with, Resource, inputs, fields
import logging
import datetime
from ..models.UserModel import *
from ..models.TripModel import *
from fields import KeyField, waypoint_fields, trip_fields, user_fields

class UserTripAPI(Resource):
    """REST API for the api/user/trip waypoint URL

    functionality for interfaceing with trips through a user
    """
    def parse_args(self):
        parser = reqparse.RequestParser()
        parser.add_argument(
            'trip_id',
            type=int,
            help='possible Key for a Trip object',
            location='json',
        )

    @marshal_with(user_fields)
    def put(self, user_id):
        """ Associated an existing Trip with a User

        Does not create a new Trip object
        """
        args = self.parse_args()
        u = User.get_by_id(user_id)
        t = Trip.get_by_id(args['trip_id'])

        # if the waypoint or trips don't exist, abort
        if u is None or t is None:
            abort(404)

        t.add_user(u)
        return u

    @marshal_with(user_fields)
    def delete(self, user_id):
        """ Removes an association from a User

        Does not delete the Trip from the database
        """
        args = self.parse_args()
        u = User.get_by_id(user_id)
        t = Trip.get_by_id(args['trip_id'])

        if u is None or t is None:
            abort(404)

        t.remove_user(u)
        return u
