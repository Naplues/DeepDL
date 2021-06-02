import torch
import torch.nn.functional as F
import torch.nn as nn
import numpy as np
from torch.autograd import Variable
import math

def mul_attention(rnn_outputs, att_weights):
    atten_vectors = None
    for i in range(rnn_outputs.size(0)):
        h_i = rnn_outputs[i]
        a_i = att_weights[i]
        h_i = a_i * h_i
        h_i = h_i.unsqueeze(0)
        if atten_vectors is None:
            atten_vectors = h_i
        else:
            atten_vectors = torch.cat((atten_vectors, h_i), 0)
    return torch.sum(atten_vectors, 0).unsqueeze(0)

def matrix_mul(input, weight, bias=False):
    feature_list = []
    for feature in input:
        feature = torch.mm(feature, weight)
        if isinstance(bias, torch.nn.parameter.Parameter):
            feature = feature + bias.expand(feature.size()[0], bias.size()[1])
        feature = torch.tanh(feature).unsqueeze(0)
        feature_list.append(feature)

    return torch.cat(feature_list, 0).squeeze()

def element_wise_mul(input1, input2):

    feature_list = []
    for feature_1, feature_2 in zip(input1, input2):
        feature_2 = feature_2.unsqueeze(1).expand_as(feature_1)
        feature = feature_1 * feature_2
        feature_list.append(feature.unsqueeze(0))
    output = torch.cat(feature_list, 0)

    return torch.sum(output, 0).unsqueeze(0)

# class AttnLayer(nn.Module):
#     def __init__(self, vocab_size, embed_size, batch_size, hidden_size):
#         super(AttnLayer, self).__init__()
#         self.word_weight = nn.Parameter(torch.Tensor(2 * hidden_size, 2 * hidden_size))
#         self.word_bias = nn.Parameter(torch.Tensor(1, 2 * hidden_size))
#         self.context_weight = nn.Parameter(torch.Tensor(2 * hidden_size, 1))
#         self.lookup = nn.Embedding(num_embeddings=vocab_size, embedding_dim=embed_size)
#         self.gru = nn.GRU(embed_size, hidden_size, bidirectional=True)
#         self._create_weights(mean=0.0, std=0.05)
#
#     def forward(self, input, hidden_state):
#         output = self.lookup(input)
#         f_output, h_output = self.gru(output.float(), hidden_state)  # feature output and hidden state output
#         output = matrix_mul(f_output, self.word_weight, self.word_bias)
#         output = matrix_mul(output, self.context_weight).permute(1, 0)
#         output = F.softmax(output)
#         output = element_wise_mul(f_output, output.permute(1, 0))
#
#         return output, h_output
#
#     def _create_weights(self, mean=0.0, std=0.05):
#         self.word_weight.data.normal_(mean, std)
#         self.context_weight.data.normal_(mean, std)


class AttnLayer(nn.Module):
    def __init__(self, vocab_size, embed_size, batch_size, hidden_size):
        super(AttnLayer, self).__init__()
        self.batch_size = batch_size
        self.embed_size = embed_size
        self.hidden_size = hidden_size
        self.attn = nn.Linear(2 * hidden_size, 2 * hidden_size)
        self.attn_combine = nn.Linear(2 * hidden_size, 2 * hidden_size, bias=False)



class PositionalEncoding(nn.Module):
    def __init__(self, d_model, dropout, code_length):
        super(PositionalEncoding, self).__init__()
        self.dropout = nn.Dropout(dropout)
        pe = torch.zeros(code_length, d_model)
        position = torch.arange(0, code_length, dtype=torch.float).unsqueeze(1)
        div_term = torch.exp(torch.arange(0, d_model, 2).float() * (-math.log(10000.0) / d_model))
        pe[:, 0::2] = torch.sin(position * div_term)
        pe[:, 1::2] = torch.cos(position * div_term)
        pe = pe.unsqueeze(0).transpose(0, 1)
        self.register_buffer('pe', pe)

    def forward(self, x):
        x = x + self.pe[:x.size(0), :]
        return self.dropout(x)


class DeepDLModel(nn.Module):
    def __init__(self, vocab_size, line_length, context_length, embed_size, nhead, hidden_size, nlayers, dropout=0.5):
        super(DeepDLModel, self).__init__()
        self.vocab_size = vocab_size
        self.line_length = line_length
        self.context_length = context_length

        self.embed_size = embed_size
        self.nhead = nhead
        self.hidden_size = hidden_size
        self.nlayers = nlayers
        self.dropout = dropout
        self.src_mask = None
        self.embedding = nn.Embedding(self.vocab_size, self.embed_size)
        self.line_pos_encoder = PositionalEncoding(self.embed_size, self.dropout, self.line_length)
        self.context_pos_encoder = PositionalEncoding(self.embed_size, self.dropout, self.context_length)
        self.target_pos_encoder = PositionalEncoding(self.embed_size, self.dropout, self.context_length)

        # Transformer Encoder
        # Encoder context
        self.context_encoder_layers = nn.TransformerEncoderLayer(self.embed_size, self.nhead, self.hidden_size, self.dropout)
        self.context_encoder = nn.TransformerEncoder(self.context_encoder_layers, self.nlayers)
        # Encoder line
        self.line_encoder_layers = nn.TransformerEncoderLayer(self.embed_size, self.nhead, self.hidden_size, self.dropout)
        self.line_encoder = nn.TransformerEncoder(self.line_encoder_layers, self.nlayers)

        # self.attn_layer = AttnLayer(self.vocab_size, self.embed_size, self.batch_size, self.hidden_size)
        # self.attn_layer = nn.MultiheadAttention(self.embed_size, self.nhead)


        # Transformer Decoder
        decoder_layers = nn.TransformerDecoderLayer(self.embed_size, self.nhead, self.hidden_size, self.dropout)
        self.decoder = nn.TransformerDecoder(decoder_layers, self.nlayers)
        self.output_layer = nn.Linear(self.embed_size, self.vocab_size)

        self._reset_parameters()



    def _generate_square_subsequent_mask(self, sz):
        mask = (torch.triu(torch.ones(sz, sz)) == 1).transpose(0, 1)
        mask = mask.float().masked_fill(mask == 0, float('-inf')).masked_fill(mask == 1, float(0.0))
        return mask

    def _reset_parameters(self):
        r"""Initiate parameters in the transformer model."""

        for p in self.parameters():
            if p.dim() > 1:
                nn.init.xavier_uniform_(p)


    def forward(self, line_input, context_input, target):
        context_embed = self.embedding(context_input)
        line_embed = self.embedding(line_input)
        target_embed = self.embedding(target)

        context_output = self.context_pos_encoder(context_embed)
        line_output = self.line_pos_encoder(line_embed)
        target_pos = self.target_pos_encoder(target_embed)

        context_output = self.context_encoder(context_output)
        line_output = self.line_encoder(line_output)
        mix_output = torch.cat((context_output, line_output),1)

        decoder_output = self.decoder(target_pos, mix_output)

        output = self.output_layer(decoder_output)



        output = F.log_softmax(output, dim=-1)
        return output.view(-1, output.size(-1))
