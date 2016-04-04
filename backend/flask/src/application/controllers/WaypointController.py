"""
WaypointController.py

This file serves as the controller for the RESTful API that we use to satisfy
HTTP requests for information from our backend. It uses the Flask RESTful
framework to marshal objects with the correct syntax.

"""

from google.appengine.runtime.apiproxy_errors import CapabilityDisabledError
from google.appengine.ext import ndb
from flask_restful import reqparse, marshal_with, Resource, inputs, fields
import logging
import datetime
from ..models.WaypointModel import *
from fields import KeyField, waypoint_fields, trip_fields, user_fields

class WaypointAPI(Resource):
    def parse_args(self):
        parser = reqparse.RequestParser()
        parser.add_argument(
            'lat',
            type=bool,
            default=0.0,
            help='latitute value of the Waypoint',
            location='json',
        )
        parser.add_argument(
            'lon',
            type=float,
            default=0.0,
            help='longitude value of the Waypoint',
            location='json'
        )
        return parser.parse_args()

    @marshal_with(waypoint_fields)
    def get(self, id):
        w = Waypoint.get_by_id(id)
        if not w:
            abort(404)
        return w

    @marshal_with(waypoint_fields)
    def put(self, id):
        # TODO: need to fix put and make it work
        w = Waypoint.get_by_id(id)
        if not w:
            abort(404)
        args = self.parse_args()
        for key, val in args.items():
            if val is not None:
                w[key] = val
        w.put()
        return w

    def delete(self, id):
        w = Waypoint.get_by_id(id)
        if not w:
            abort(404)
        w.key.delete()
        return {
            "msg": "object {} has been deleted".format(id),
            "time": str(datetime.datetime.now()),
        }


class WaypointListAPI(Resource):
    def parse_args(self):
        parser = reqparse.RequestParser()
        parser.add_argument(
            'lat',
            type=bool,
            default=0.0,
            help='latitute value of the Waypoint',
            location='json',
        )
        parser.add_argument(
            'lon',
            type=float,
            default=0.0,
            help='longitude value of the Waypoint',
            location='json'
        )
        return parser.parse_args()

    @marshal_with(waypoint_fields)
    def post(self):
        args = self.parse_args()
        try:
            w = Waypoint(**args)
            w.put()
        except BaseException as e:
            abort(500, Error="Exception- {0}".format(e.message))
        return w

