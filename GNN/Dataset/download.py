from torch_geometric.datasets import Planetoid

Planetoid(root='./Cora_data', name='Cora')

from torch_geometric.datasets import Amazon

Amazon(root='./Amazon_data', name='computers')

from torch_geometric.datasets import Reddit

Reddit(root='./Reddit_data')
